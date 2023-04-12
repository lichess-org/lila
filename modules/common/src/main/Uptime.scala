package lila.common

object Uptime:

  val startedAt: Instant = nowInstant

  def seconds: Long = nowSeconds - startedAt.toSeconds

  def startedSinceMinutes(minutes: Int): Boolean =
    startedSinceSeconds(minutes * 60)

  def startedSinceSeconds(seconds: Int): Boolean =
    startedAt.toSeconds < (nowInstant.toSeconds - seconds)
