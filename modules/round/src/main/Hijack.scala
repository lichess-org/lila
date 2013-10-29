package lila.round

import lila.game.Pov
import lila.user.Context
import lila.memo.ExpireSetMemo

import scala.concurrent.duration.Duration

private[round] final class Hijack(timeout: Duration) {

  // full game ids that have been hijacked
  private val hijacks = new ExpireSetMemo(timeout)

  def apply(pov: Pov, token: String): Boolean = pov.game.rated && {
    if (hijacks get pov.fullId) true
    else if (token != pov.game.token) { 
      logwarn(s"[websocket] hijacking detected ${pov.fullId}")
      hijacks put pov.fullId
      true
    }
    else false
  }
}
