package lila.game

import chess.Color

// times are expressed in seconds
case class CorrespondenceClock(
    increment: Int,
    whiteTime: Float,
    blackTime: Float) {

  def emerg = 60 * 10

  def remainingTime(c: Color) = c.fold(whiteTime, blackTime)

  def outoftime(c: Color) = remainingTime(c) == 0
}
