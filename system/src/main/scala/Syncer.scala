package lila.system

import lila.chess.Color
import model._
import memo._
import db.GameRepo
import scalaz.effects._
import scala.annotation.tailrec
import scala.math.max
import org.apache.commons.lang3.StringEscapeUtils.escapeXml

final class Syncer(
    gameRepo: GameRepo,
    versionMemo: VersionMemo,
    aliveMemo: AliveMemo,
    duration: Int,
    sleep: Int) {

  def sync(
    gameId: String,
    colorString: String,
    version: Int,
    fullId: Option[String]): IO[Map[String, Any]] = {
    for {
      color ← io { Color(colorString) err "Invalid color" }
      _ ← versionWait(gameId, color, version)
      gameAndPlayer ← gameRepo.player(gameId, color)
      (game, player) = gameAndPlayer
      isPrivate = fullId some { game.isPlayerFullId(player, _) } none false
      _ ← versionMemo put game
    } yield {
      player.eventStack eventsSince version map { events ⇒
        Map(
          "v" -> player.eventStack.lastVersion,
          "e" -> renderEvents(events, isPrivate),
          "p" -> game.player.color.name,
          "t" -> game.turns,
          "oa" -> aliveMemo.activity(game, !color),
          "c" -> (game.clock some { clock ⇒
            clock.remainingTimes mapKeys (_.name)
          } none null)
        ) filterValues (null !=)
      } getOrElse failMap
    }
  } except (e ⇒ io(failMap))

  private def renderEvents(events: List[Event], isPrivate: Boolean) =
    if (isPrivate) events map {
      case MessageEvent(author, message) ⇒ renderMessage(author, message)
      case e                             ⇒ e.export
    }
    else events filter {
      case MessageEvent(_, _) | RedirectEvent(_) ⇒ false
      case _                                     ⇒ true
    } map (_.export)

  // TODO author=system messages should be translated!!
  private def renderMessage(author: String, message: String) = Map(
    "type" -> "message",
    "html" -> """<li class="%s">%s</li>""".format(author, escapeXml(message))
  )

  private def versionWait(gameId: String, color: Color, version: Int) = io {
    @tailrec
    def wait(loop: Int): Unit = {
      if (loop == 0 || versionMemo.get(gameId, color) != version) ()
      else { Thread sleep sleep; wait(loop - 1) }
    }
    wait(max(1, duration / sleep))
  }

  private val failMap = Map("reload" -> true)
}
