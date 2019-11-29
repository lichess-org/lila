package lila.common

import org.joda.time.DateTime
import scala.jdk.CollectionConverters._

object PlayApp {

  val startedAt = DateTime.now
  val startedAtMillis = nowMillis

  def uptimeSeconds = nowSeconds - startedAt.getSeconds

  def startedSinceMinutes(minutes: Int) =
    startedSinceSeconds(minutes * 60)

  def startedSinceSeconds(seconds: Int) =
    startedAtMillis < (nowMillis - (seconds * 1000))
}
