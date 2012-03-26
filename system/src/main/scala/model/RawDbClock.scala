package lila.system
package model

import lila.chess._
import scala.math.round

case class RawDbClock(
    color: String,
    increment: Int,
    limit: Int,
    white: Float,
    black: Float,
    timer: Double = 0d) {

  def decode: Option[Clock] = for {
    trueColor ← Color(color)
  } yield {
    if (timer == 0l) PausedClock(
      color = trueColor,
      increment = increment,
      limit = limit,
      whiteTime = white,
      blackTime = black)
    else RunningClock(
      color = trueColor,
      increment = increment,
      limit = limit,
      whiteTime = white,
      blackTime = black,
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
      white = whiteTime,
      black = blackTime,
      timer = timer
    )
  }
}
