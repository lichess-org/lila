package lila.playban

import org.joda.time.DateTime

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
  } option TempBan.make(bans)
}

case class TempBan(
    date: DateTime,
    mins: Int
) {

  def endsAt = date plusMinutes mins

  def remainingSeconds: Int = (endsAt.getSeconds - nowSeconds).toInt atLeast 0

  def remainingMinutes: Int = (remainingSeconds / 60) atLeast 1

  def inEffect = endsAt isAfter DateTime.now

}

object TempBan {
  private def make(minutes: Int) = TempBan(
    DateTime.now,
    minutes atMost 48 * 60
  )

  /**
   * Create a playban. First offense: 15 min.
   * Multiplier of repeat offense after X days:
   * - 0 days: 3x
   * - 0 - 5 days: linear scale from 3x to 1x
   * - >5 days slow drop off
   */
  def make(bans: List[TempBan]): TempBan = make(bans.lastOption.fold(15) { prev =>
    prev.endsAt.toNow.getStandardHours.truncInt match {
      case h if h < 120 => prev.mins * (180 - h) / 60
      case h => {
        // Scale cooldown period by total number of playbans
        val t = (Math.sqrt(bans.size) * 20 * 24).toInt
        (prev.mins * (t + 120 - h) / t) atLeast 30
      }
    }
  })
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
