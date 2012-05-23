package lila
package monitor

import play.api.libs.concurrent.Promise
import java.util.concurrent.TimeUnit
import scala.concurrent.stm._
import scala.math.round

final class RpsProvider(timeout: Int) {

  private val counter = Ref((0, (0, nowMillis)))

  def countRequest() = {
    val current = nowMillis
    counter.single.transform {
      case (precedent, (count, millis)) if current > millis + timeout       ⇒ (0, (1, current))
      case (precedent, (count, millis)) if current > millis + (timeout / 2) ⇒ (count, (1, current))
      case (precedent, (count, millis))                                     ⇒ (precedent, (count + 1, millis))
    }
  }

  def rps = round {
    val current = nowMillis
    val (precedent, (count, millis)) = counter.single()
    val since = current - millis
    if (since <= timeout) ((count + precedent) * 1000) / (since + timeout / 2)
    else 0
  } toInt

}
