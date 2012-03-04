package lila.system
package model

import lila.chess._

case class RawDbClock(
    color: String,
    increment: Int,
    limit: Int,
    times: Map[String, Float]) {

  def decode: Option[Clock] = for {
    trueColor ← Color(color)
    whiteTime ← times get "white"
    blackTime ← times get "black"
  } yield Clock(
    color = trueColor,
    increment = increment,
    limit = limit,
    whiteTime = whiteTime,
    blackTime = blackTime
  )
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
      )
    )
  }
}
