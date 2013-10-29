package lila.round

import scala.concurrent.duration.Duration

import lila.game.Pov
import lila.memo.ExpireSetMemo
import lila.user.Context

private[round] final class Hijack(timeout: Duration, enabled: Boolean) {

  // full game ids that have been hijacked
  private val hijacks = new ExpireSetMemo(timeout)

  def apply(pov: Pov, token: String): Boolean = enabled && {
    pov.game.rated && {
      if (hijacks get pov.fullId) true
      else if (token != pov.game.token) {
        logwarn(s"[websocket] hijacking detected ${pov.fullId}")
        hijacks put pov.fullId
        true
      }
      else false
    }
  }
}
