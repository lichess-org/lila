package lila
package game

import chess._
import scala.math.round

case class RawDbClock(
    c: Boolean,
    i: Int,
    l: Int,
    w: Float,
    b: Float,
    t: Option[Double] = None) {

  def decode: Clock = t.fold(
    PausedClock(
      color = Color(c),
      increment = i,
      limit = l,
      whiteTime = w,
      blackTime = b): Clock) { timer ⇒
      RunningClock(
        color = Color(c),
        increment = i,
        limit = l,
        whiteTime = w,
        blackTime = b,
        timer = timer)
    }
}

object RawDbClock {

  def encode(clock: Clock): RawDbClock = {
    import clock._
    RawDbClock(
      c = color.white,
      i = increment,
      l = limit,
      w = whiteTime,
      b = blackTime,
      t = timerOption
    )
  }
}
