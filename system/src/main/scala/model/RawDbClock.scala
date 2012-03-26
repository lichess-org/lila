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
      increment = increment * 1000,
      limit = limit * 1000,
      whiteTime = round(white * 1000),
      blackTime = round(black * 1000))
    else RunningClock(
      color = trueColor,
      increment = increment * 1000,
      limit = limit * 1000,
      whiteTime = round(white * 1000),
      blackTime = round(black * 1000),
      timer = (timer * 1000).toLong)
  }
}

object RawDbClock {

  def encode(clock: Clock): RawDbClock = {
    import clock._
    println(clock)
    RawDbClock(
      color = color.name,
      increment = increment / 1000,
      limit = limit / 1000,
      white = whiteTime / 1000f,
      black = blackTime / 1000f,
      timer = timer / 1000d
    ).pp
  }
}
