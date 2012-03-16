package lila.system

import lila.chess.Color
import model._
import scalaz.effects._

final class Syncer(repo: GameRepo) {

  def sync(
    id: String,
    colorString: String,
    version: Int,
    fullId: String): IO[Map[String, Any]] = {
    for {
      color ← io { Color(colorString) err "Invalid color" }
      gameAndPlayer ← repo.player(id, color)
      (g, p) = gameAndPlayer
      //events ← io { eventsSince(p, version) }
    } yield Map(
      "v" -> p.eventStack.version,
      //"e" -> events map (_.toMap),
      "p" -> g.player,
      "t" -> g.turns
    )
  } except (e ⇒ io(Map("reload" -> true)))

  def eventsSince(player: DbPlayer, version: Int): List[Event] =
    player.eventStack.eventsSince(version) | Nil

}
