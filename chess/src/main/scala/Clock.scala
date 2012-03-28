package lila.chess

import scala.math.max

// All durations are expressed in milliseconds
sealed trait Clock {
  val limit: Int
  val increment: Int
  val color: Color
  val whiteTime: Float
  val blackTime: Float
  val timer: Double

  def time(c: Color) = if (c == White) whiteTime else blackTime

  def outoftime(c: Color) = remainingTime(c) == 0

  def remainingTime(c: Color) = max(0, limit - elapsedTime(c))

  def remainingTimes = Color.all map { c ⇒ c -> remainingTime(c) } toMap

  def elapsedTime(c: Color) = time(c)

  def limitInSeconds = limit / 1000

  def limitInMinutes = limitInSeconds / 60

  def incrementInSeconds = increment / 1000

  def estimateTotalTime = limit + 30 * increment

  def step: RunningClock

  protected def now = System.currentTimeMillis / 1000d
}

case class RunningClock(
    limit: Int,
    increment: Int,
    color: Color = White,
    whiteTime: Float = 0f,
    blackTime: Float = 0f,
    timer: Double = 0d) extends Clock {

  override def elapsedTime(c: Color) = time(c) + {
    if (c == color) now - timer else 0
  }.toFloat

  def step = {
    val t = now
    addTime(
      color,
      max(0, (t - timer).toFloat - Clock.httpDelay - increment)
    ).copy(
        color = !color,
        timer = t
      )
  }

  def addTime(c: Color, t: Float) = c match {
    case White ⇒ copy(whiteTime = whiteTime + t)
    case Black ⇒ copy(blackTime = blackTime + t)
  }

  def giveTime(c: Color, t: Float) = addTime(c, -t)
}

case class PausedClock(
    limit: Int,
    increment: Int,
    color: Color = White,
    whiteTime: Float = 0f,
    blackTime: Float = 0f) extends Clock {

  val timer = 0d

  def step = RunningClock(
    color = color,
    whiteTime = whiteTime,
    blackTime = blackTime,
    increment = increment,
    limit = limit,
    timer = now).giveTime(White, increment).step
}

object Clock {

  val httpDelay = 0.5f
}
