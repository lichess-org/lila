package lila.plan

import org.joda.time.DateTime
import reactivemongo.api.bson.BSONNull

import lila.db.dsl._

final private class MonthlyGoalApi(getGoal: () => Usd, chargeColl: Coll)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  def get: Fu[MonthlyGoal] =
    monthAmount dmap { amount =>
      MonthlyGoal(current = amount, goal = getGoal())
    }

  private def monthAmount: Fu[Usd] =
    chargeColl
      .aggregateWith() { framework =>
        import framework._
        List(
          Match($doc("date" $gt DateTime.now.withDayOfMonth(1).withTimeAtStartOfDay)),
          Group(BSONNull)("usd" -> SumField("usd"))
        )
      }
      .headOption
      .map {
        _.flatMap { _.getAsOpt[BigDecimal]("usd") } | BigDecimal(0)
      } dmap Usd.apply
}

case class MonthlyGoal(current: Usd, goal: Usd) {

  def percent = (100 * current.value / goal.value).toInt
}
