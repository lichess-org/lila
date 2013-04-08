package lila.round

import lila.game.Game
import lila.memo.ExpireSetMemo

import scala.concurrent.duration.Duration

private[round] final class FinisherLock(timeout: Duration) {

  private val cache = new ExpireSetMemo(timeout)

  def isLocked(game: Game): Boolean = cache get game.id

  def lock(game: Game) { cache put game.id }
}
