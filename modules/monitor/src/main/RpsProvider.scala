package lila.monitor

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.stm.Ref

private[monitor] final class RpsProvider(timeout: FiniteDuration) {

  private val counter = Ref((0, (0, nowMillis)))

  private val tms = timeout.toMillis

  def add {
    val current = nowMillis
    counter.single.transform {
      case (precedent, (count, millis)) if current > millis + tms       => (0, (1, current))
      case (precedent, (count, millis)) if current > millis + (tms / 2) => (count, (1, current))
      case (precedent, (count, millis))                                 => (precedent, (count + 1, millis))
    }
  }

  def rps = {
    val current = nowMillis
    val (precedent, (count, millis)) = counter.single()
    val since = current - millis
    if (since <= tms) ((count + precedent) * 1000) / (since + tms / 2)
    else 0
  } toInt

}
