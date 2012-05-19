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

  def offerOrAccept(fullId: String): IO[Valid[(String, List[Event])]] =
    attempt(fullId, {
      case pov @ Pov(game, color) if game playerCanRematch color ⇒
        if (game.opponent(color).isOfferingRematch) success {
          game.nextId.fold(
            nextId ⇒ io(nextId -> Nil),
            for {
              nextGame ← returnGame(pov) map (_.start)
              _ ← gameRepo insert nextGame
              nextId = nextGame.id
              _ ← game.variant.standard.fold(io(), gameRepo saveInitialFen game)
              _ ← timelinePush(game)
              // messenges are not sent to the next game socket
              // as nobody is there to see them yet
              _ ← messenger.systemMessages(nextGame, List(
                Some(nextGame.creatorColor + " creates the game"),
                Some(nextGame.invitedColor + " joins the game"),
                nextGame.clock map Namer.clock,
                nextGame.rated option "This game is rated").flatten)
            } yield nextId -> List(
              Event.RedirectOwner(White, playerUrl(nextGame, White)),
              Event.RedirectOwner(Black, playerUrl(nextGame, Black)),
              // tell spectators to reload the table
              Event.ReloadTable(White),
              Event.ReloadTable(Black))
          )
        }
        else success {
          val progress = Progress(game, Event.ReloadTable(!color)) map { g ⇒
            g.updatePlayer(color, _.offerRematch)
          }
          gameRepo save progress map { _ ⇒ fullId -> progress.events }
        }
      case _ ⇒ !!("invalid rematch offer " + fullId)
    })

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
        userId ⇒ userRepo user userId map { userOption ⇒
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
