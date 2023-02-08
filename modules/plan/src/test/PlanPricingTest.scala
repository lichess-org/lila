package lila.plan

import org.specs2.mutable.Specification

class PlanPricingTest extends Specification {

  "nicely round" >> {
    import PlanPricingApi.nicelyRound
    "round to nice number" >> {
      val ns = {
        def next(i: Double, j: Double): List[Double] =
          // if (i > 1_000_000) List(i)
          if (i > 30_000) List(i)
          else i :: next(i + j + i / 10, j + 1)
        next(0, 0.01)
      }
      println(ns.map(n => (n, nicelyRound(n))).mkString("\n"))
      // List(0, 0.9, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
      // nicelyRound(0) === 0
      // nicelyRound(0.9) === 1
      // nicelyRound(1) === 1
      // nicelyRound(1.1) === 1
      // nicelyRound(1.3) === 1
      // nicelyRound(1.7) === 2
      // nicelyRound(3) === 2
      // nicelyRound(4) === 5
      // nicelyRound(6) === 5
      // nicelyRound(9) === 10
      // nicelyRound(1009) === 1000
      nicelyRound(1) === 1
    }
  }

}
