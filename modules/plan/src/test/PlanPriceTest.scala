package lila.plan

import org.specs2.mutable.Specification

class PlanPriceTest extends Specification {

  "nicely round" in {
    import PlanPriceApi.nicelyRound
    "round to 1-2-5 series" in {
      nicelyRound(0) must_== 0
      nicelyRound(0.01) must_== 0.01
      nicelyRound(0.03) must_== 0.05
      nicelyRound(1) must_== 1
      nicelyRound(1.1) must_== 1
      nicelyRound(1.3) must_== 1.5
      nicelyRound(1.7) must_== 1.5
      nicelyRound(1.8) must_== 2
      nicelyRound(3) must_== 5
      nicelyRound(9) must_== 10
      nicelyRound(1009) must_== 1000
    }
  }

}
