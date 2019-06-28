package lila.plan

import org.joda.time.DateTime
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson.BSONNull

import lila.db.dsl._

private final class MonthlyGoalApi(getGoal: () => Usd, chargeColl: Coll) {

  def get: Fu[MonthlyGoal] = monthAmount map { amount =>
    MonthlyGoal(current = amount, goal = getGoal().cents)
  }

  def monthAmount: Fu[Cents] =
    chargeColl.aggregateOne(
      Match($doc("date" $gt DateTime.now.withDayOfMonth(1).withTimeAtStartOfDay)), List(
        Group(BSONNull)("cents" -> SumField("cents"))
      )
    ).map {
        ~_.flatMap { _.getAs[Int]("cents") }
      } map Cents.apply
}

case class MonthlyGoal(current: Cents, goal: Cents) {

  def percent = 100 * current.value / goal.value
}
