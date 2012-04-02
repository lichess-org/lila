package lila.system

import lila.chess.Color
import model._
import memo._
import db.GameRepo
import scalaz.effects._
import scala.annotation.tailrec
import scala.math.max

final class AppSyncer(
    gameRepo: GameRepo,
    versionMemo: VersionMemo,
    aliveMemo: AliveMemo,
    duration: Int,
    sleep: Int) {

  def sync(
    gameId: String,
    colorString: String,
    version: Int,
    fullId: Option[String]): IO[Option[Map[String, Any]]] = Color(colorString).fold(
    color ⇒ for {
      _ ← versionWait(gameId, color, version)
      povOption ← gameRepo.povOption(gameId, color)
      _ ← povOption.fold(pov ⇒ versionMemo put pov.game, io())
    } yield povOption map { pov ⇒
      pov.player.eventStack eventsSince version some { events ⇒
        Map(
          "v" -> pov.player.eventStack.lastVersion,
          "e" -> renderEvents(events, pov isPlayerFullId fullId),
          "p" -> pov.game.player.color.name,
          "t" -> pov.game.turns,
          "oa" -> aliveMemo.activity(pov.game, !color),
          "c" -> (pov.game.clock some { clock ⇒
            clock.remainingTimes mapKeys (_.name)
          } none null)
        ) filterValues (null !=)
      } none Map("reload" -> true)
    },
    io(None)
  )

  private def renderEvents(events: List[Event], isPrivate: Boolean) =
    if (isPrivate) events map {
      case MessageEvent(author, message) ⇒ renderMessage(author, message)
      case e                             ⇒ e.export
    }
    else events filter {
      case MessageEvent(_, _) | RedirectEvent(_) ⇒ false
      case _                                     ⇒ true
    } map (_.export)

  private def renderMessage(author: String, message: String) = Map(
    "type" -> "message",
    "html" -> Room.render(author, message)
  )

  private def versionWait(gameId: String, color: Color, version: Int) = io {
    @tailrec
    def wait(loop: Int): Unit = {
      if (loop == 0 || versionMemo.get(gameId, color) != version) ()
      else { Thread sleep sleep; wait(loop - 1) }
    }
    wait(max(1, duration / sleep))
  }
}
