package lila
package game

import chess._
import scala.math.round

private[game] case class RawDbClock(
    c: Boolean,
    i: Int,
    l: Int,
    w: Float,
    b: Float,
    t: Option[Double] = None) {

  def decode: Clock = t.fold(
    timer ⇒ RunningClock(
      color = Color(c),
      increment = i,
      limit = l,
      whiteTime = w,
      blackTime = b,
      timer = timer),
    PausedClock(
      color = Color(c),
      increment = i,
      limit = l,
      whiteTime = w,
      blackTime = b)
  )
}

private[game] object RawDbClock {

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
