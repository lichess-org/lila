package lila.game

import scala.concurrent.duration._

import lila.db.ByteArray

case class ClockHistory(
    binaryMoveTimes: Option[ByteArray] = None
) {

  def moveTimes(playedTurns: Int): Option[Vector[FiniteDuration]] =
    binaryMoveTimes.map(binary => BinaryFormat.moveTime.read(binary).take(playedTurns))

  def withTime(playedTurns: Int, moveTime: FiniteDuration) =
    ClockHistory(
      binaryMoveTimes = BinaryFormat.moveTime.write(~moveTimes(playedTurns) :+ moveTime).some
    )

  def take(turns: Int) = ClockHistory(
    binaryMoveTimes = moveTimes(turns).map(BinaryFormat.moveTime write _)
  )
}

object ClockHistory {

  val empty = ClockHistory()
}
