package lila.playban

import play.api.libs.json.*

import lila.common.Json.given

case class UserRecord(
    _id: UserId,
    o: Option[Vector[Outcome]],
    b: Option[Vector[TempBan]],
    c: Option[RageSit]
):

  inline def userId                    = _id
  inline def outcomes: Vector[Outcome] = ~o
  inline def bans: Vector[TempBan]     = ~b
  inline def rageSit                   = c | RageSit.empty

  def banInEffect = bans.lastOption.exists(_.inEffect)
  def banMinutes  = bans.lastOption.map(_.remainingMinutes)

  def nbOutcomes = outcomes.size

  def badOutcomeScore: Float =
    outcomes.collect {
      case Outcome.NoPlay | Outcome.Abort => .7f
      case o if o != Outcome.Good         => 1
    } sum

  def badOutcomeRatio: Float = if bans.sizeIs < 3 then 0.4f else 0.3f

  def minBadOutcomes: Int =
    bans.size match
      case 0 | 1 => 4
      case 2 | 3 => 3
      case _     => 2

  def badOutcomesStreakSize: Int =
    bans.size match
      case 0     => 6
      case 1 | 2 => 5
      case _     => 4

  def bannable(accountCreationDate: Instant): Option[TempBan] = {
    rageSitRecidive || {
      outcomes.lastOption.exists(_ != Outcome.Good) && {
        // too many bad overall
        badOutcomeScore >= (badOutcomeRatio * nbOutcomes atLeast minBadOutcomes.toFloat) || {
          // bad result streak
          outcomes.sizeIs >= badOutcomesStreakSize &&
          outcomes.takeRight(badOutcomesStreakSize).forall(Outcome.Good !=)
        }
      }
    }
  } option TempBan.make(bans, accountCreationDate)

  def rageSitRecidive =
    outcomes.lastOption.exists(Outcome.rageSitLike.contains) && {
      rageSit.isTerrible || {
        rageSit.isVeryBad && outcomes.count(Outcome.rageSitLike.contains) > 1
      } || {
        rageSit.isBad && outcomes.count(Outcome.rageSitLike.contains) > 2
      }
    }

case class TempBan(date: Instant, mins: Int):

  def endsAt = date plusMinutes mins

  def remainingSeconds: Int = (endsAt.toSeconds - nowSeconds).toInt atLeast 0

  def remainingMinutes: Int = (remainingSeconds / 60) atLeast 1

  def inEffect = endsAt.isAfterNow

object TempBan:

  given Writes[TempBan] = Json.writes

  private def make(minutes: Int) =
    TempBan(
      nowInstant,
      minutes atMost 3 * 24 * 60
    )

  private val baseMinutes = 10

  /** Create a playban. First offense: 10 min. Multiplier of repeat offense after X days:
    *   - 0 days: 3x
    *   - 0 - 3 days: linear scale from 3x to 1x
    *   - >3 days quick drop off Account less than 3 days old --> 2x the usual time
    */
  def make(bans: Vector[TempBan], accountCreationDate: Instant): TempBan =
    make {
      (bans.lastOption so { prev =>
        prev.endsAt.toNow.toHours.toSaturatedInt match
          case h if h < 72 => prev.mins * (132 - h) / 60
          case h           => (55.6 * prev.mins / (Math.pow(5.56 * prev.mins - 54.6, h / 720) + 54.6)).toInt
      } atLeast baseMinutes) * (if accountCreationDate.plusDays(3).isAfterNow then 2 else 1)
    }

enum Outcome(val id: Int, val name: String):

  case Good      extends Outcome(0, "Nothing unusual")
  case Abort     extends Outcome(1, "Aborts the game")
  case NoPlay    extends Outcome(2, "Won't play a move")
  case RageQuit  extends Outcome(3, "Quits without resigning")
  case Sitting   extends Outcome(4, "Lets time run out")
  case SitMoving extends Outcome(5, "Waits then moves at last moment")
  case Sandbag   extends Outcome(6, "Deliberately lost the game")
  case SitResign extends Outcome(7, "Waits then resigns at last moment")

  val key = lila.common.String.lcfirst(toString)

object Outcome:

  val rageSitLike: Set[Outcome] = Set(RageQuit, Sitting, SitMoving, SitResign)

  val byId = values.mapBy(_.id)

  def apply(id: Int): Option[Outcome] = byId get id
