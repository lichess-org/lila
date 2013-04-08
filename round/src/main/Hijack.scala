package lila.round

import lila.game.Pov
import lila.user.Context
import lila.memo.ExpireSetMemo

import scala.concurrent.duration.Duration
import play.api.libs.concurrent.Execution.Implicits._

private[round] final class Hijack(timeout: Duration) {

  // full game ids that have been hijacked
  private val hijacks = new ExpireSetMemo(timeout)

  def apply(pov: Pov, token: String, ctx: Context) =
    if (hijacks get pov.fullId) true
    else if (token != pov.game.token) true ~ { _ ⇒
      loginfo("[websocket] hijacking detected %s %s".format(pov.fullId, ctx.toString))
      hijacks put pov.fullId
    }
    else false
}
