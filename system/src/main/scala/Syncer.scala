package lila.system

import lila.chess.Color
import model._
import scalaz.effects._

final class Syncer(repo: GameRepo) {

  val reload = Map("reload" -> true)

  def sync(
    id: String,
    colorString: String,
    version: Int,
    fullId: String): IO[Map[String, Any]] = {
    for {
      color ← io { Color(colorString) err "Invalid color" }
      gameAndPlayer ← repo.player(id, color)
      (g, p) = gameAndPlayer
    } yield {
      p.eventStack eventsSince version map { events ⇒
        Map(
          "v" -> p.eventStack.lastVersion,
          "e" -> (events map (_.export)),
          "p" -> g.player.color.name,
          "t" -> g.turns
        )
      } getOrElse reload
    }
  } except (e ⇒ io(reload))

}
