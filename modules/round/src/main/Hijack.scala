package lila.round

import scala.concurrent.duration._

import lila.game.{ Pov, Game, IdGenerator }
import lila.memo.ExpireSetMemo
import lila.user.Context

private[round] final class Hijack(timeout: Duration, enabled: Boolean) {

  // game ID -> game token
  private val tokens = lila.memo.Builder.cache(
    2 hour, 
    (_: String) ⇒ IdGenerator.token
  )

  // full game ids that have been hijacked
  private val hijacks = new ExpireSetMemo(timeout)

  def tokenOf(gameId: String) = tokens get gameId

  def apply(pov: Pov, token: String): Boolean = enabled && {
    pov.game.rated && {
      if (hijacks get pov.fullId) true
      else if (token != tokenOf(pov.game.id)) {
        logwarn(s"[websocket] hijacking detected ${pov.fullId}")
        hijacks put pov.fullId
        true
      }
      else false
    }
  }
}
