package lila
package setup

import chess.{ Game, Board, Clock, Variant, Color ⇒ ChessColor }
import ChessColor.{ White, Black }
import chess.format.Forsyth
import game.{ GameRepo, DbGame, DbPlayer, Pov, Handler, Namer, Source }
import round.{ Event, Progress, Messenger }
import user.UserRepo
import controllers.routes

import scalaz.effects._

private[setup] final class Rematcher(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    messenger: Messenger,
    timelinePush: DbGame ⇒ IO[Unit]) extends Handler(gameRepo) {

  type Result = (String, List[Event])

  def offerOrAccept(fullId: String): IO[Valid[Result]] =
    attempt(fullId, {
      case pov @ Pov(game, color) if game playerCanRematch color ⇒
        success(game.opponent(color).isOfferingRematch.fold(
          game.next.fold(rematchJoin(pov))(rematchExists(pov)),
          rematchCreate(pov)
        ))
      case _ ⇒ !!("invalid rematch offer " + fullId)
    })

  private def rematchExists(pov: Pov)(nextId: String): IO[Result] =
    gameRepo.pov(nextId, !pov.color) map { nextPovOption ⇒
      nextPovOption.fold(pov.fullId -> Nil)(_.fullId -> Nil)
    }

  private def rematchJoin(pov: Pov): IO[Result] = for {
    nextGame ← returnGame(pov) map (_.start)
    _ ← gameRepo insert nextGame
    nextId = nextGame.id
    _ ← gameRepo denormalize nextGame
    _ ← gameRepo.saveNext(pov.game, nextGame.id)
    _ ← timelinePush(nextGame)
    // messenges are not sent to the next game socket
    // as nobody is there to see them yet
    _ ← messenger.rematch(pov.game, nextGame)
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
    pieces ← pov.game.variant.standard.fold(
      io(pov.game.variant.pieces),
      pov.game.is960Rematch.fold(
        io(Variant.Chess960.pieces),
        gameRepo initialFen pov.game.id map { fenOption ⇒
          (fenOption flatMap Forsyth.<< map { situation ⇒
            situation.board.pieces
          }) | pov.game.variant.pieces
        }
      )
    )
    whitePlayer ← returnPlayer(pov.game, White)
    blackPlayer ← returnPlayer(pov.game, Black)
  } yield DbGame.apply(
    game = Game(
      board = Board(pieces, variant = pov.game.variant),
      clock = pov.game.clock map (_.reset)),
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    ai = None,
    creatorColor = !pov.color,
    mode = pov.game.mode,
    variant = pov.game.variant,
    source = pov.game.source | Source.Lobby,
    pgnImport = None) with960Rematch !pov.game.is960Rematch

  private def returnPlayer(game: DbGame, color: ChessColor): IO[DbPlayer] =
    DbPlayer(color = color, aiLevel = None) |> { player ⇒
      game.player(!color).userId.fold(io(player)) { userId ⇒
        userRepo byId userId map { _.fold(player)(player.withUser) }
      }
    }

  private def clockName(clock: Option[Clock]): String =
    clock.fold("Unlimited")(Namer.clock)

  private def playerUrl(game: DbGame, color: ChessColor): String =
    routes.Round.player(game fullIdOf color).url
}
