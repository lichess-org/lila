package lila.common

object Uptime:

  val startedAt: DateTime   = nowDate
  val startedAtMillis: Long = nowMillis

  def seconds: Long = nowSeconds - startedAt.toSeconds

  def startedSinceMinutes(minutes: Int): Boolean =
    startedSinceSeconds(minutes * 60)

  def startedSinceSeconds(seconds: Int): Boolean =
    startedAtMillis < (nowMillis - (seconds * 1000))
