package lila.game

import scala.concurrent.duration._

import lila.db.ByteArray

case class ClockHistory(
    binaryMoveTimes: Option[ByteArray] = None
) {

  lazy val moveTimes: Option[Vector[FiniteDuration]] =
    binaryMoveTimes.map(BinaryFormat.moveTime read _)

  def :+(moveTime: FiniteDuration) = ClockHistory(
    binaryMoveTimes = BinaryFormat.moveTime.write((moveTimes | Vector.empty) :+ moveTime).some
  )

  def take(turns: Int) = ClockHistory(
    binaryMoveTimes = moveTimes.map(BinaryFormat.moveTime write _.take(turns))
  )

  def totalTime = moveTimes.map(_.fold(0 millis)(_ + _))
}

object ClockHistory {

  val empty = ClockHistory()
}
