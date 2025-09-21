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

  val zero: PatronMonths = 0
  val lifetime: PatronMonths = PatronTier.Lifetime.months

  extension (months: PatronMonths)
    def isOngoing: Boolean = months > 0
    def isLifetime: Boolean = tier.contains(PatronTier.Lifetime)
    def tier: Option[PatronTier] = PatronTier(months)

enum PatronStyle:
  case months1, months2, months3, months6, months9,
        years1, years2, years3, years4, years5,
        lifetime
  def key = toString
  def cssClass = toString

object PatronStyle:
  val map: Map[String, PatronStyle] = values.mapBy(_.cssClass)

enum PatronTier(val months: Int, val style: PatronStyle, val name: String):
  case Months1 extends PatronTier(1, PatronStyle.months1, "1 month")
  case Months2 extends PatronTier(2, PatronStyle.months2, "2 months")
  case Months3 extends PatronTier(3, PatronStyle.months3, "3 months")
  case Months6 extends PatronTier(6, PatronStyle.months6, "6 months")
  case Months9 extends PatronTier(9, PatronStyle.months9, "9 months")
  case Years1 extends PatronTier(12, PatronStyle.years1, "1 year")
  case Years2 extends PatronTier(24, PatronStyle.years2, "2 years")
  case Years3 extends PatronTier(36, PatronStyle.years3, "3 years")
  case Years4 extends PatronTier(48, PatronStyle.years4, "4 years")
  case Years5 extends PatronTier(60, PatronStyle.years5, "5 years")
  case Lifetime extends PatronTier(999, PatronStyle.lifetime, "Lifetime")

object PatronTier:

  private val reverseValues = values.reverse.toList

  def apply(months: PatronMonths): Option[PatronTier] =
    reverseValues.find(_.months <= months.value)

  case class AndStyle(tier: PatronTier, custom: Option[PatronStyle]):
    def style = custom | tier.style
