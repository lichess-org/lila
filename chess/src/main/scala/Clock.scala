package lila.chess

import scala.math.max

// All durations are expressed in milliseconds
sealed trait Clock {
  val limit: Int
  val increment: Int
  val color: Color
  val whiteTime: Int
  val blackTime: Int
  val timer: Long

  def time(c: Color) = if (c == White) whiteTime else blackTime

  def isOutOfTime(c: Color) = remainingTime(c) == 0

  def remainingTime(c: Color) = max(limit, elapsedTime(c))

  def remainingTimes = Color.all map { c ⇒ (c, remainingTime(c)) }

  def elapsedTime(c: Color) = time(c)

  def limitInSeconds = limit / 1000

  def limitInMinutes = limitInSeconds / 60

  def estimateTotalTime = limit + 30 * increment

  def step: RunningClock

  override def toString =
    "%d minutes/side + %d seconds/move".format(limitInMinutes, increment)
}

case class RunningClock(
    limit: Int,
    increment: Int,
    color: Color = White,
    whiteTime: Int = 0,
    blackTime: Int = 0,
    timer: Long = 0l) extends Clock {

  override def elapsedTime(c: Color) = time(c) + {
    if (c == color) (now - timer).toInt else 0
  }

  def step =
    addTime(
      color,
      max(0, (now - timer).toInt - Clock.httpDelay - increment)
    ).copy(
      color = !color,
      timer = now
    )

  def addTime(c: Color, t: Int) = c match {
    case White ⇒ copy(whiteTime = whiteTime + t)
    case Black ⇒ copy(blackTime = blackTime + t)
  }

  def giveTime(c: Color, t: Int) = addTime(c, -t)

  private def now = System.currentTimeMillis
}

case class PausedClock(
    limit: Int,
    increment: Int,
    color: Color = White,
    whiteTime: Int = 0,
    blackTime: Int = 0) extends Clock {

  val timer = 0l

  def step = RunningClock(
    color = color,
    whiteTime = whiteTime,
    blackTime = blackTime,
    increment = increment,
    limit = limit).giveTime(White, increment).step
}

object Clock {

  val httpDelay = 500
}
