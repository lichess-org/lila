package lila.system

import lila.chess.Color
import model._
import memo._
import scalaz.effects._
import scala.annotation.tailrec
import scala.math.max

final class Syncer(
    repo: GameRepo,
    versionMemo: VersionMemo,
    duration: Int,
    sleep: Int) {

  def sync(
    gameId: String,
    colorString: String,
    version: Int,
    fullId: String): IO[Map[String, Any]] = {
    for {
      color ← io { Color(colorString) err "Invalid color" }
      _ ← io { versionWait(gameId, color, version) }
      gameAndPlayer ← repo.player(gameId, color)
      (game, player) = gameAndPlayer
      _ ← versionMemo put game
    } yield {
      player.eventStack eventsSince version map { events ⇒
        Map(
          "v" -> player.eventStack.lastVersion,
          "e" -> (events map (_.export)),
          "p" -> game.player.color.name,
          "t" -> game.turns
        )
      } getOrElse failMap
    }
  } except (e ⇒ io(failMap))

  private def versionWait(gameId: String, color: Color, version: Int) {
    @tailrec
    def wait(loop: Int): Unit = {
      if (loop == 0 || versionMemo.get(gameId, color) != version) ()
      else { Thread sleep sleep; wait(loop - 1) }
    }
    wait(max(1, duration / sleep))
  }

  //private val failMap = Map("failMap" -> true)
  private val failMap = Map("fuck" -> true)
}
