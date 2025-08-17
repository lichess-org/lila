package lila.core
package plan

import lila.core.userId.{ UserId, UserName }

case class ChargeEvent(username: UserName, cents: Int, percent: Int, date: Instant)
case class MonthInc(userId: UserId, months: Int)
case class PlanStart(userId: UserId)
case class PlanGift(from: UserId, to: UserId, lifetime: Boolean)
case class PlanExpire(userId: UserId)
