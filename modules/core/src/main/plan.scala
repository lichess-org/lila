package lila.core
package plan

import lila.core.userId.{ UserId, UserName }

case class ChargeEvent(username: UserName, cents: Int, percent: Int, date: Instant)
case class MonthInc(userId: UserId, months: Int)
case class PlanStart(userId: UserId)
case class PlanGift(from: UserId, to: UserId, lifetime: Boolean)
case class PlanExpire(userId: UserId)

opaque type PatronMonths = Int // 0 if no plan is ongoing, 999 if lifetime
object PatronMonths extends OpaqueInt[PatronMonths]:

  val zero = PatronMonths(0)

  extension (months: PatronMonths)
    def isOngoing: Boolean = months > 0
    def isLifetime: Boolean = tier.contains(PatronTier.Lifetime)
    def tier: Option[PatronTier] = PatronTier(months)

enum PatronTier(val months: Int, val key: String, val name: String):

  case Months1 extends PatronTier(1, "months1", "1 month")
  case Months2 extends PatronTier(2, "months2", "2 months")
  case Months3 extends PatronTier(3, "months3", "3 months")
  case Months6 extends PatronTier(6, "months6", "6 months")
  case Months9 extends PatronTier(9, "months9", "9 months")
  case Years1 extends PatronTier(12, "years1", "1 year")
  case Years2 extends PatronTier(24, "years2", "2 years")
  case Years3 extends PatronTier(36, "years3", "3 years")
  case Years4 extends PatronTier(48, "years4", "4 years")
  case Years5 extends PatronTier(60, "years5", "5 years")
  case Lifetime extends PatronTier(999, "lifetime", "Lifetime")

object PatronTier:

  private val reverseValues = values.reverse.toList

  def apply(months: PatronMonths): Option[PatronTier] =
    reverseValues.find(_.months <= months.value)
