package lila.playban

import org.joda.time.{ DateTime, Duration }

case class UserRecord(
    _id: String,
    o: Option[List[Outcome]],
    b: Option[List[TempBan]]
) {

  def userId = _id
  def outcomes: List[Outcome] = ~o
  def bans: List[TempBan] = ~b

  def banInEffect = bans.lastOption.exists(_.inEffect)

  lazy val nbOutcomes = outcomes.size

  lazy val nbBadOutcomes = outcomes.count(_ != Outcome.Good)

  def badOutcomeRatio: Double =
    if (nbOutcomes == 0) 0
    else nbBadOutcomes.toDouble / nbOutcomes

  def nbBadOutcomesBeforeBan = if (bans.isEmpty) 4 else 3

  def bannable: Option[TempBan] = {
    nbBadOutcomes >= nbBadOutcomesBeforeBan && badOutcomeRatio >= 1d / 2
  } option bans.lastOption.fold(TempBan.initial) { prev =>
    new Duration(prev.endsAt, DateTime.now).toStandardDays.getDays match {
      case d if d < 3 => TempBan.make(prev.mins * 3)
      case d => TempBan.make((prev.mins / Math.log(d)).toInt atLeast 30)
    }
  }
}

case class TempBan(
    date: DateTime,
    mins: Int
) {

  lazy val endsAt = date plusMinutes mins

  def remainingSeconds: Int = (endsAt.getSeconds - nowSeconds).toInt max 0

  def remainingMinutes: Int = (remainingSeconds / 60) max 1

  def inEffect = endsAt isAfter DateTime.now
}

object TempBan {
  val initialMinutes = 15
  def initial = make(initialMinutes)
  def make(minutes: Int) = TempBan(DateTime.now, minutes atMost 60 * 48)
}

sealed abstract class Outcome(
    val id: Int,
    val name: String
)

object Outcome {

  case object Good extends Outcome(0, "Nothing unusual")
  case object Abort extends Outcome(1, "Aborts the game")
  case object NoPlay extends Outcome(2, "Won't play a move")
  case object RageQuit extends Outcome(3, "Quits without resigning")
  case object Sitting extends Outcome(4, "Lets time run out")
  case object SitMoving extends Outcome(5, "Waits then moves at last moment")
  case object Sandbag extends Outcome(6, "Deliberately lost the game")

  val all = List(Good, Abort, NoPlay, RageQuit, Sitting, SitMoving, Sandbag)

  val byId = all map { v => (v.id, v) } toMap

  def apply(id: Int): Option[Outcome] = byId get id
}
