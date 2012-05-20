package lila
package setup

import chess.{ Color ⇒ ChessColor }
import game.{ GameRepo, DbGame, Pov }
import user.User
import round.{ Event, Progress, Messenger }
import controllers.routes

import com.mongodb.DBRef
import scalaz.effects._

final class Joiner(
    gameRepo: GameRepo,
    messenger: Messenger,
    timelinePush: DbGame ⇒ IO[Unit],
    dbRef: User ⇒ DBRef) {

  def apply(game: DbGame, user: Option[User]): Valid[IO[(Pov, List[Event])]] =
    game.notStarted option {
      val color = game.invitedColor
      for {
        p1 ← user.fold(
          u ⇒ gameRepo.setUser(game.id, color, dbRef(u), u.elo) map { _ ⇒
            Progress(game, game.updatePlayer(color, _.withUser(u, dbRef(u))))
          },
          io(Progress(game)))
        p2 = p1 withGame game.start
        p3 ← messenger init game map { evts ⇒
          p2 + Event.RedirectOwner(!color, playerUrl(game, !color)) ++ evts
        }
        _ ← gameRepo save p3
      } yield Pov(game, color) -> p3.events
    } toSuccess ("Can't join started game " + game.id).wrapNel

  private def playerUrl(game: DbGame, color: ChessColor): String =
    routes.Round.player(game fullIdOf color).url
}
