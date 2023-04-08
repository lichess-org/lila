package lila.common

object Uptime:

  val startedAt       = nowDate
  val startedAtMillis = nowMillis

  def seconds = nowSeconds - startedAt.getSeconds

  def startedSinceMinutes(minutes: Int) =
    startedSinceSeconds(minutes * 60)

  def startedSinceSeconds(seconds: Int) =
    startedAtMillis < (nowMillis - (seconds * 1000))
