package lila.playban

import org.joda.time.DateTime

case class UserRecord(
    _id: String,
    o: Option[List[Outcome]],
    b: Option[List[TempBan]]) {

  def userId = _id
  def outcomes: List[Outcome] = ~o
  def bans: List[TempBan] = ~b

  def banInEffect = bans.lastOption.??(_.inEffect)

  lazy val nbOutcomes = outcomes.size

  lazy val nbBadOutcomes = outcomes.count(_ != Outcome.Good)

  def badOutcomeRatio: Double =
    if (nbOutcomes == 0) 0
    else nbBadOutcomes.toDouble / nbOutcomes

  def nbBadOutcomesBeforeBan = if (bans.isEmpty) 3 else 2

  def newBan: Option[TempBan] = {
    !banInEffect &&
      nbBadOutcomes >= nbBadOutcomesBeforeBan &&
      badOutcomeRatio >= 1d / 4
  } option bans.lastOption.filterNot(_.isOld).fold(TempBan.initial) { prev =>
    TempBan(prev.mins * 2)
  }
}

case class TempBan(
    date: DateTime,
    mins: Int) {

  lazy val endsAt = date plusMinutes mins

  def remainingSeconds: Int = (endsAt.getSeconds - nowSeconds).toInt max 0

  def remainingMinutes: Int = (remainingSeconds / 60) max 1

  def inEffect = endsAt isAfter DateTime.now

  def isOld = date isBefore DateTime.now.minusDays(3)
}

object TempBan {
  val initialMinutes = 15
  def initial = apply(initialMinutes)
  def apply(minutes: Int): TempBan = TempBan(DateTime.now, minutes min 120)
}

sealed abstract class Outcome(
  val id: Int,
  val name: String)

object Outcome {

  case object Good extends Outcome(0, "Nothing unusual")
  case object Abort extends Outcome(1, "Aborts the game")
  case object NoPlay extends Outcome(2, "Won't play a move")
  case object RageQuit extends Outcome(3, "Quits without resigning")
  case object Sitting extends Outcome(4, "Lets time run out")

  val all = List(Good, Abort, NoPlay, RageQuit, Sitting)

  val byId = all map { v => (v.id, v) } toMap

  def apply(id: Int): Option[Outcome] = byId get id
}
