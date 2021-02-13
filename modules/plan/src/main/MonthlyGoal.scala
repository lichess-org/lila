package lila.plan

import org.joda.time.DateTime
import reactivemongo.api.bson.BSONNull

import lila.db.dsl._

final private class MonthlyGoalApi(getGoal: () => Usd, chargeColl: Coll)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  def get: Fu[MonthlyGoal] =
    monthAmount dmap { amount =>
      MonthlyGoal(current = amount, goal = getGoal().cents)
    }

  def monthAmount: Fu[Cents] =
    chargeColl
      .aggregateWith() { framework =>
        import framework._
        List(
          Match($doc("date" $gt DateTime.now.withDayOfMonth(1).withTimeAtStartOfDay)),
          Group(BSONNull)("cents" -> SumField("cents"))
        )
      }
      .headOption
      .map {
        ~_.flatMap { _.int("cents") }
      } dmap Cents.apply
}

case class MonthlyGoal(current: Cents, goal: Cents) {

  def percent = 100 * current.value / goal.value
}
