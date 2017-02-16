package lila.game

import scala.concurrent.duration._

import lila.db.ByteArray

case class ClockHistory(
    binaryMoveTimes: ByteArray = ByteArray.empty
) {

  lazy val moveTimes: Vector[FiniteDuration] =
    BinaryFormat.moveTime read binaryMoveTimes

  def :+(moveTime: FiniteDuration) = ClockHistory(
    binaryMoveTimes = BinaryFormat.moveTime write moveTimes :+ moveTime
  )

  def take(turns: Int) = ClockHistory(
    binaryMoveTimes = BinaryFormat.moveTime write moveTimes.take(turns)
  )

  def totalTime = moveTimes.fold(0 millis)(_ + _)
}

object ClockHistory {

  val empty = ClockHistory()
}
