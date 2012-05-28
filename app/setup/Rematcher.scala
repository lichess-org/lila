package lila
package setup

import chess.{ Game, Board, Clock, Color ⇒ ChessColor }
import ChessColor.{ White, Black }
import chess.format.Forsyth
import game.{ GameRepo, DbGame, DbPlayer, Pov, Handler, Namer }
import round.{ Event, Progress, Messenger }
import user.UserRepo
import controllers.routes

import scalaz.effects._

final class Rematcher(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    messenger: Messenger,
    timelinePush: DbGame ⇒ IO[Unit]) extends Handler(gameRepo) {

  type Result = (String, List[Event])

  def offerOrAccept(fullId: String): IO[Valid[Result]] =
    attempt(fullId, {
      case pov @ Pov(game, color) if game playerCanRematch color ⇒
        success(game.opponent(color).isOfferingRematch.fold(
          game.nextId.fold(
            rematchExists(pov),
            rematchJoin(pov)
          ),
          rematchCreate(pov)
        ))
      case _ ⇒ !!("invalid rematch offer " + fullId)
    })

  private def rematchExists(pov: Pov)(nextId: String): IO[Result] =
    gameRepo.pov(nextId, !pov.color) map { nextPovOption ⇒
      nextPovOption.fold(
        _.fullId -> Nil,
        pov.fullId -> Nil)
    }

  private def rematchJoin(pov: Pov): IO[Result] = for {
    nextGame ← returnGame(pov) map (_.start)
    _ ← gameRepo insert nextGame
    nextId = nextGame.id
    _ ← gameRepo denormalizeStarted pov.game
    _ ← timelinePush(nextGame)
    // messenges are not sent to the next game socket
    // as nobody is there to see them yet
    _ ← messenger init nextGame
  } yield (nextGame fullIdOf !pov.color) -> List(
    Event.RedirectOwner(White, playerUrl(nextGame, Black)),
    Event.RedirectOwner(Black, playerUrl(nextGame, White)),
    // tell spectators to reload the table
    Event.ReloadTable(White),
    Event.ReloadTable(Black))

  private def rematchCreate(pov: Pov): IO[Result] = for {
    p1 ← messenger.systemMessage(pov.game, _.rematchOfferSent) map { es ⇒
      Progress(pov.game, Event.ReloadTable(!pov.color) :: es)
    }
    p2 = p1 map { g ⇒ g.updatePlayer(pov.color, _ offerRematch) }
    _ ← gameRepo save p2
  } yield pov.fullId -> p2.events

  private def returnGame(pov: Pov): IO[DbGame] = for {
    board ← pov.game.variant.standard.fold(
      io(pov.game.variant.pieces),
      gameRepo initialFen pov.game.id map { fenOption ⇒
        (fenOption flatMap Forsyth.<< map { situation ⇒
          situation.board.pieces
        }) | pov.game.variant.pieces
      })
    whitePlayer ← returnPlayer(pov.game, White)
    blackPlayer ← returnPlayer(pov.game, Black)
  } yield DbGame(
    game = Game(
      board = Board(board),
      clock = pov.game.clock map (_.reset)),
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    ai = None,
    creatorColor = !pov.color,
    mode = pov.game.mode,
    variant = pov.game.variant)

  private def returnPlayer(game: DbGame, color: ChessColor): IO[DbPlayer] =
    DbPlayer(color = color, aiLevel = None) |> { player ⇒
      game.player(color).userId.fold(
        userId ⇒ userRepo byId userId map { userOption ⇒
          userOption.fold(
            user ⇒ player.withUser(user)(userRepo.dbRef),
            player)
        },
        io(player))
    }

  private def clockName(clock: Option[Clock]): String =
    clock.fold(Namer.clock, "Unlimited")

  private def playerUrl(game: DbGame, color: ChessColor): String =
    routes.Round.player(game fullIdOf color).url
}
