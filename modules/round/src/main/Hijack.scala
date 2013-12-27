package lila.round

import scala.concurrent.duration._

import com.roundeights.hasher.Implicits._

import lila.game.{ Pov, Game, IdGenerator }
import lila.memo.ExpireSetMemo

private[round] final class Hijack(
    timeout: Duration,
    salt: String,
    enabled: Boolean) {

  // full game ids that have been hijacked
  private val hijacks = new ExpireSetMemo(timeout)

  def tokenOf(gameId: String) = gameId.salt(salt).md5.hex take 8

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
