package lila.memo

import scala.concurrent.duration.FiniteDuration
import play.api.mvc.Result
import play.api.mvc.Results.TooManyRequest
import play.api.libs.json.Json

/**
 * only allow one future at a time per key
 */
final class LinearLimit[K](
    name: String,
    key: String,
    ttl: FiniteDuration,
    limitedDefault: Fu[Result] = fuccess(TooManyRequest(Json.obj("error" -> "Try again later"))),
    toString: K => String = (k: K) => k.toString
) {
  private val storage = new ExpireSetMemo(ttl)

  private val logger = lila.log("linearlimit")
  private val monitor = lila.mon.security.linearLimit.generic(key)

  logger.info(s"[start] $name")

  def apply(k: K, msg: => String = "", limited: => Fu[Result] = limitedDefault)(f: => Fu[Result]): Fu[Result] =
    if (storage get toString(k)) {
      logger.info(s"$name $k $msg")
      limited
    } else {
      storage put toString(k)
      f addEffectAnyway {
        storage remove toString(k)
      }
    }
}
