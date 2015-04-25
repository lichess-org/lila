package lila.playban

import org.joda.time.DateTime

case class UserRecord(
    _id: String,
    o: List[Outcome],
    b: List[TempBan]) {

  def userId = _id
  def outcomes = o
  def bans = b

  def banInEffect = bans.lastOption.??(_.inEffect)

  lazy val nbOutcomes = outcomes.size

  lazy val nbBadOutcomes = outcomes.count(_ != Outcome.Good)

  def badOutcomeRatio: Double =
    if (nbOutcomes == 0) 0
    else nbBadOutcomes.toDouble / nbOutcomes

  def nbBadOutcomesBeforeBan = if (bans.isEmpty) 5 else 3

  def newBan: Option[TempBan] = {
    !banInEffect &&
      nbBadOutcomes >= nbBadOutcomesBeforeBan &&
      badOutcomeRatio > 1d / 3
  } option bans.lastOption.filterNot(_.isOld).fold(TempBan(5)) { prev =>
    TempBan(prev.mins * 2)
  }
}

case class TempBan(
    date: DateTime,
    mins: Int) {

  lazy val endsAt = date plusMinutes mins

  def remainingSeconds: Int = (endsAt.getSeconds - nowSeconds).toInt max 0

  def inEffect = endsAt isBefore DateTime.now

  def isOld = date isBefore DateTime.now.minusDays(1)
}

object TempBan {
  def apply(minutes: Int): TempBan = TempBan(DateTime.now, minutes min 120)
}

sealed abstract class Outcome(
  val id: Int,
  val name: String)

object Outcome {

  case object Good extends Outcome(0, "Nothing unusual")
  case object Abort extends Outcome(1, "Aborts the game")
  case object NoPlay extends Outcome(2, "Won't play a move")

  val all = List(Good, Abort, NoPlay)

  val byId = all map { v => (v.id, v) } toMap

  def apply(id: Int): Option[Outcome] = byId get id
}
