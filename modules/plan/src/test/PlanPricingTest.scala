package lila.plan

import org.specs2.mutable.Specification

class PlanPricingTest extends Specification {

  "nicely round" in {
    import PlanPricingApi.nicelyRound
    "round to nice number" in {
      val ns = {
        def next(i: Double, j: Double): List[Double] =
          // if (i > 1_000_000) List(i)
          if (i > 30_000) List(i)
          else i :: next(i + j + i / 10, j + 1)
        next(0, 0.01)
      }
      println(ns.map(n => (n, nicelyRound(n))).mkString("\n"))
      // List(0, 0.9, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
      // nicelyRound(0) must_== 0
      // nicelyRound(0.9) must_== 1
      // nicelyRound(1) must_== 1
      // nicelyRound(1.1) must_== 1
      // nicelyRound(1.3) must_== 1
      // nicelyRound(1.7) must_== 2
      // nicelyRound(3) must_== 2
      // nicelyRound(4) must_== 5
      // nicelyRound(6) must_== 5
      // nicelyRound(9) must_== 10
      // nicelyRound(1009) must_== 1000
      nicelyRound(1) must_== 1
    }
  }

}
