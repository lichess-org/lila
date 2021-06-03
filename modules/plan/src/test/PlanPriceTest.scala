package lila.plan

import org.specs2.mutable.Specification

class PlanPriceTest extends Specification {

  "nicely round" in {
    import PlanPriceApi.nicelyRound
    "round to 1-2-5 series" in {
      nicelyRound(0) must_== 0
      nicelyRound(0.00003) must_== 0.00002
      nicelyRound(0.01) must_== 0.01
      nicelyRound(0.03) must_== 0.02
      nicelyRound(0.04) must_== 0.05
      nicelyRound(0.14) must_== 0.1
      nicelyRound(0.16) must_== 0.2
      nicelyRound(1) must_== 1
      nicelyRound(1.1) must_== 1
      nicelyRound(1.3) must_== 1
      nicelyRound(1.7) must_== 2
      nicelyRound(3) must_== 2
      nicelyRound(4) must_== 5
      nicelyRound(6) must_== 5
      nicelyRound(9) must_== 10
      nicelyRound(1009) must_== 1000
      nicelyRound(1600) must_== 2000
    }
  }

}
