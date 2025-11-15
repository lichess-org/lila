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

enum PatronColor(val id: Int):
  case color1 extends PatronColor(1)
  case color2 extends PatronColor(2)
  case color3 extends PatronColor(3)
  case color4 extends PatronColor(4)
  case color5 extends PatronColor(5)
  case color6 extends PatronColor(6)
  case color7 extends PatronColor(7)
  case color8 extends PatronColor(8)
  case color9 extends PatronColor(9)
  case color10 extends PatronColor(10)
  def cssClass = s"paco$id"
  def selectable(tier: PatronTier) = id <= tier.color.id

object PatronColor:
  val map: Map[Int, PatronColor] = values.mapBy(_.id)

// color manually chosen by the user
opaque type PatronColorChoice = PatronColor
object PatronColorChoice:
  extension (color: PatronColorChoice) def value: PatronColor = color
  def apply(color: PatronColor): PatronColorChoice = color
  def resolve(choice: Option[PatronColorChoice], default: PatronColor): PatronColorResolved =
    choice | default

// color to display, either chosen by the user or the default for their tier
opaque type PatronColorResolved = PatronColor
object PatronColorResolved:
  extension (color: PatronColorResolved) def value: PatronColor = color

enum PatronTier(val months: Int, val color: PatronColor, val name: String):
  case Months1 extends PatronTier(1, PatronColor.color1, "1 month")
  case Months2 extends PatronTier(2, PatronColor.color2, "2 months")
  case Months3 extends PatronTier(3, PatronColor.color3, "3 months")
  case Months6 extends PatronTier(6, PatronColor.color4, "6 months")
  case Months9 extends PatronTier(9, PatronColor.color5, "9 months")
  case Years1 extends PatronTier(12, PatronColor.color6, "1 year")
  case Years2 extends PatronTier(24, PatronColor.color7, "2 years")
  case Years3 extends PatronTier(36, PatronColor.color8, "3 years")
  case Years4 extends PatronTier(48, PatronColor.color9, "4 years")
  case Years5 extends PatronTier(60, PatronColor.color10, "5 years")
  case Lifetime extends PatronTier(999, PatronColor.color10, "Lifetime")

object PatronTier:

  private val reverseValues = values.reverse.toList

  def apply(months: PatronMonths): Option[PatronTier] =
    reverseValues.find(_.months <= months.value)

  def byColor(color: PatronColor): PatronTier =
    values.find(_.color == color).getOrElse(Months1)

  case class AndColor(tier: PatronTier, choice: Option[PatronColorChoice]):
    def color: PatronColorResolved = PatronColorChoice.resolve(choice, tier.color)
