package lila
package model

import chess._
import scala.math.round

case class RawDbClock(
    c: String,
    i: Int,
    l: Int,
    w: Float,
    b: Float,
    timer: Option[Double] = None) {

  def decode: Option[Clock] = for {
    trueColor ← Color(c)
  } yield {
    timer.fold(
      t ⇒ RunningClock(
        color = trueColor,
        increment = i,
        limit = l,
        whiteTime = w,
        blackTime = b,
        timer = t),
      PausedClock(
        color = trueColor,
        increment = i,
        limit = l,
        whiteTime = w,
        blackTime = b)
    )
  }
}

object RawDbClock {

  def encode(clock: Clock): RawDbClock = {
    import clock._
    RawDbClock(
      c = color.name,
      i = increment,
      l = limit,
      w = whiteTime,
      b = blackTime,
      timer = timerOption
    )
  }
}
