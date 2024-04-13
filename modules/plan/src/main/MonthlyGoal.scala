package lila.plan

import reactivemongo.api.bson.BSONNull

import lila.db.dsl.{ *, given }

final private class MonthlyGoalApi(getGoal: () => Usd, chargeColl: Coll)(using Executor):

  def get: Fu[MonthlyGoal] =
    monthAmount.dmap: amount =>
      MonthlyGoal(current = amount, goal = getGoal())

  private def monthAmount: Fu[Usd] =
    chargeColl
      .aggregateWith(): framework =>
        import framework.*
        List(
          Match($doc("date".$gt(nowInstant.dateTime.withDayOfMonth(1).withTimeAtStartOfDay))),
          Group(BSONNull)("usd" -> SumField("usd"))
        )
      .headOption
      .map { _.flatMap(_.getAsOpt[Usd]("usd")) | Usd(0) }

case class MonthlyGoal(current: Usd, goal: Usd):

  def percent = (goal.value > 0).so((100 * current.value / goal.value).toInt)
