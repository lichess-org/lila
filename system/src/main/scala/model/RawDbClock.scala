package lila.system
package model

import lila.chess._

case class RawDbClock(
    color: String,
    increment: Int,
    limit: Int,
    times: Map[String, Long],
    timer: Long = 0l) {

  def decode: Option[Clock] = for {
    trueColor ← Color(color)
    whiteTime ← times get "white"
    blackTime ← times get "black"
  } yield {
    if (timer == 0l) PausedClock(
      color = trueColor,
      increment = increment,
      limit = limit,
      whiteTime = whiteTime,
      blackTime = blackTime)
    else RunningClock(
      color = trueColor,
      increment = increment,
      limit = limit,
      whiteTime = whiteTime,
      blackTime = blackTime,
      timer = timer)
  }
}

object RawDbClock {

  def encode(clock: Clock): RawDbClock = {
    import clock._
    RawDbClock(
      color = color.name,
      increment = increment,
      limit = limit,
      times = Map(
        "white" -> whiteTime,
        "black" -> blackTime
      ),
      timer = timer
    )
  }
}
