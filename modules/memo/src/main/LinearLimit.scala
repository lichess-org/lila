package lidraughts.memo

import ornicar.scalalib.Zero
import scala.concurrent.duration.FiniteDuration

/**
 * only allow one future at a time per key
 */
final class LinearLimit(
    name: String,
    key: String,
    ttl: FiniteDuration
) {
  private val storage = new ExpireSetMemo(ttl)

  private val logger = lidraughts.log("linearlimit")
  private val monitor = lidraughts.mon.security.linearLimit.generic(key)

  logger.info(s"[start] $name")

  def apply[A](k: String, msg: => String = "")(f: => Fu[A]): Option[Fu[A]] =
    if (storage get k) {
      logger.info(s"$name $k $msg")
      none
    } else Some {
      storage put k
      f addEffectAnyway {
        storage remove k
      }
    }
}
