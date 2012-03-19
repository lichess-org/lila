package lila.system
package memo

import model._
import lila.chess.{ Color, White, Black }
import scalaz.effects._

final class AliveMemo(hardTimeout: Int, softTimeout: Int) {

  private val cache = Builder.expiry[String, Long](hardTimeout)

  private val bigLatency = 999 * 1000

  def get(gameId: String, color: Color): Option[Long] = Option {
    cache getIfPresent toKey(gameId, color)
  }

  def put(key: String): IO[Unit] = io {
    cache.put(key, now)
  }

  def put(gameId: String, color: Color): IO[Unit] = io {
    cache.put(toKey(gameId, color), now)
  }

  def put(gameId: String, color: Color, time: Long): IO[Unit] = io {
    cache.put(toKey(gameId, color), time)
  }

  def transfer(g1: String, c1: Color, g2: String, c2: Color): IO[Unit] = io {
    get(g1, c1) foreach { put(g2, c2, _) }
  }

  def latency(gameId: String, color: Color): Int =
    get(gameId, color) some { time ⇒ (now - time).toInt } none bigLatency

  /**
   * Get player activity (or connectivity)
   * 2 - good connectivity
   * 1 - recently offline
   * 0 - offline for long time
   */
  def activity(game: DbGame, color: Color): Int =
    if (game.player(color).isAi) 2
    else activity(game.id, color)

  def activity(gameId: String, color: Color): Int =
    latency(gameId, color) |> { l ⇒
      if (l <= softTimeout) 2
      else if (l <= hardTimeout) 1
      else 0
    }

  def count = cache.size

  def toKey(gameId: String, color: Color) = gameId + "." + color.letter

  private def now = System.currentTimeMillis
}
