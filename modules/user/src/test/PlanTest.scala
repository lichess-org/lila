package lila.user

import org.specs2.mutable.Specification

class PlanTest extends Specification {

  "empty" in {
    Plan.empty.isEmpty must beTrue
  }

  "new patron" in {
    Plan.start.isEmpty must beFalse
    Plan.start.months == 1 must beTrue
  }

  "continuing patron" in {
    val plan = Plan.start.incMonths
    plan.isEmpty must beFalse
    plan.months == 2 must beTrue
  }

  "lapsed patron" in {
    val oneMonthPlan = Plan.start.disable
    oneMonthPlan.isEmpty must beFalse
    oneMonthPlan.months == 1 must beTrue
    val twoMonthPlan = Plan.start.incMonths.disable
    twoMonthPlan.isEmpty must beFalse
    twoMonthPlan.months == 2 must beTrue
  }

  "returning patron" in {
    val twoMonthPlan = Plan.start.disable.incMonths
    twoMonthPlan.isEmpty must beFalse
    twoMonthPlan.months == 2 must beTrue
    val threeMonthPlan = Plan.start.incMonths.disable.incMonths
    threeMonthPlan.isEmpty must beFalse
    threeMonthPlan.months == 3 must beTrue
  }
}
