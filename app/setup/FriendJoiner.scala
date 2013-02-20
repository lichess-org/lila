package lila
package setup

import chess.{ Color ⇒ ChessColor }
import game.{ GameRepo, DbGame, Pov }
import user.User
import round.{ Event, Progress, Messenger }
import controllers.routes

import scalaz.effects._

final class FriendJoiner(
    gameRepo: GameRepo,
    messenger: Messenger,
    timelinePush: DbGame ⇒ IO[Unit]) {

  def apply(game: DbGame, user: Option[User]): Valid[IO[(Pov, List[Event])]] =
    game.notStarted option {
      val color = game.invitedColor
      for {
        p1 ← user.fold(
          u ⇒ gameRepo.setUser(game.id, color, u) map { _ ⇒
            Progress(game, game.updatePlayer(color, _ withUser u))
          },
          io(Progress(game)))
        p2 = p1 map (_.start)
        p3 ← messenger init p2.game map { evts ⇒
          p2 + Event.RedirectOwner(!color, playerUrl(p2.game, !color)) ++ evts
        }
        _ ← gameRepo save p3
        _ ← gameRepo denormalize p3.game
      } yield Pov(p3.game, color) -> p3.events
    } toValid "Can't join started game " + game.id

  private def playerUrl(game: DbGame, color: ChessColor): String =
    routes.Round.player(game fullIdOf color).url
}
