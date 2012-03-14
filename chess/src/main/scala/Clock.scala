package lila.chess

import scala.math.max

sealed trait Clock {
  val color: Color
  val whiteTime: Long
  val blackTime: Long
  val increment: Int
  val limit: Int
  val timer: Long

  def time(c: Color) = if (c == White) whiteTime else blackTime

  def isOutOfTime(c: Color) = remainingTime(c) == 0

  def remainingTime(c: Color) = max(limit, elapsedTime(c))

  def remainingTimes = Color.all map { c ⇒ (c, remainingTime(c)) }

  def elapsedTime(c: Color) = time(c)

  def limitInMinutes = limit / 60

  def now = System.currentTimeMillis

  def estimateTotalTime = limit + 30 * increment

  override def toString =
    "%d minutes/side + %d seconds/move".format(limitInMinutes, increment)
}

case class RunningClock(
    color: Color = White,
    whiteTime: Long = 0l,
    blackTime: Long = 0l,
    timer: Long = 0l,
    increment: Int,
    limit: Int) extends Clock {

  override def elapsedTime(c: Color) = time(c) + {
    if (c == color) now - timer else 0
  }

  def step =
    addTime(color, max(0, now - timer - Clock.httpDelay - increment)).copy(
      color = !color,
      timer = now
    )

  def addTime(c: Color, t: Long) = c match {
    case White ⇒ copy(whiteTime = whiteTime + t)
    case Black ⇒ copy(blackTime = blackTime + t)
  }

  def giveTime(c: Color, t: Long) = addTime(c, -t)
}

case class PausedClock(
    color: Color = White,
    whiteTime: Long = 0l,
    blackTime: Long = 0l,
    increment: Int,
    limit: Int) extends Clock {

      val timer = 0l
}

object Clock {

  val httpDelay = 500
}
