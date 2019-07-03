package lila.playban

import org.joda.time.DateTime
import play.api.libs.json._

case class UserRecord(
    _id: String,
    o: Option[List[Outcome]],
    b: Option[List[TempBan]],
    c: Option[Int]
) {

  def userId = _id
  def outcomes: List[Outcome] = ~o
  def bans: List[TempBan] = ~b
  def sitAndDcCounter: Int = ~c

  def banInEffect = bans.lastOption.exists(_.inEffect)

  def nbOutcomes = outcomes.size

  def badOutcomeScore: Float = outcomes.collect {
    case Outcome.NoPlay | Outcome.Abort => .7f
    case o if o != Outcome.Good => 1
  } sum

  def badOutcomeRatio: Float = if (bans.size < 3) 0.4f else 0.3f

  def minBadOutcomes: Int = bans.size match {
    case 0 | 1 => 4
    case 2 | 3 => 3
    case _ => 2
  }

  def badOutcomesStreakSize: Int = bans.size match {
    case 0 => 6
    case 1 | 2 => 5
    case _ => 4
  }

  def bannable(accountCreationDate: DateTime): Option[TempBan] = {
    outcomes.lastOption.exists(_ != Outcome.Good) && {
      // too many bad overall
      badOutcomeScore >= (badOutcomeRatio * nbOutcomes atLeast minBadOutcomes) || {
        // bad result streak
        outcomes.size >= badOutcomesStreakSize &&
          outcomes.takeRight(badOutcomesStreakSize).forall(Outcome.Good !=)
      }
    }
  } option TempBan.make(bans, accountCreationDate)
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

  implicit val tempbanWrites = Json.writes[TempBan]

  private def make(minutes: Int) = TempBan(
    DateTime.now,
    minutes atMost 48 * 60
  )

  private val baseMinutes = 10

  /**
   * Create a playban. First offense: 10 min.
   * Multiplier of repeat offense after X days:
   * - 0 days: 3x
   * - 0 - 3 days: linear scale from 3x to 1x
   * - >3 days quick drop off
   * Account less than 3 days old --> 2x the usual time
   */
  def make(bans: List[TempBan], accountCreationDate: DateTime): TempBan = make {
    (bans.lastOption ?? { prev =>
      prev.endsAt.toNow.getStandardHours.truncInt match {
        case h if h < 72 => prev.mins * (132 - h) / 60
        case h => prev.mins - Math.pow(h / 12, 1.5).toInt
      }
    } atLeast baseMinutes) * (if (accountCreationDate.plusDays(3).isAfter(DateTime.now)) 2 else 1)
  }
}

sealed abstract class Outcome(
    val id: Int,
    val name: String
) {
  val key = toString.head.toLower + toString.tail
}

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
