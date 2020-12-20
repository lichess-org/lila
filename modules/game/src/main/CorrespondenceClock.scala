package lila.game

import chess.Color

// times are expressed in seconds
case class CorrespondenceClock(
    increment: Int,
    whiteTime: Float,
    blackTime: Float
) {

  import CorrespondenceClock._

  def daysPerTurn = increment / 60 / 60 / 24

  def remainingTime(c: Color) = c.fold(whiteTime, blackTime)

  def outoftime(c: Color) = remainingTime(c) == 0

  def moretimeable(c: Color) = remainingTime(c) < (increment - hourSeconds)

  def giveTime(c: Color) =
    c.fold(
      copy(whiteTime = whiteTime + daySeconds),
      copy(blackTime = blackTime + daySeconds)
    )

  // in seconds
  def estimateTotalTime = increment * 40 / 2

  def incrementHours = increment / 60 / 60
}

private object CorrespondenceClock {

  private val hourSeconds = 60 * 60
  private val daySeconds  = 24 * hourSeconds
}
