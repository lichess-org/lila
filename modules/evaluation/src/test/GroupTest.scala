package lila.evaluation
package grouping

import org.specs2.specification._
import org.specs2.mutable._

import scalaz.NonEmptyList

class GroupTest extends Specification {
  val game1 = GameGroup(
        NonEmptyList(15, 40, 10, 30, 20, 40, 20, 80, 30, 30, 10, 40, 20, 60, 100, 80, 50, 30, 50, 5, 100, 300, 150, 50, 60, 20, 15, 15, 15, 60, 30, 80, 80, 150, 80, 200, 10, 80, 60, 150, 50, 30, 30, 150, 40, 40, 30, 30),
        NonEmptyList(0, 1, 2, 1),
        List(),
        List()
      )
  val game2 = GameGroup(
    NonEmptyList(15, 40, 10, 30, 20, 40, 20, 80, 30, 30, 10, 40, 20, 60, 100, 80, 50, 30, 50, 5, 100, 300, 150, 50, 60, 20, 15, 15, 15, 60, 30, 80, 80, 150, 80, 200, 10, 80, 60, 150, 50, 30, 30, 150, 40, 40, 30, 30),
    NonEmptyList(0, 1, 2, 1),
    List(),
    List()
  )

  "Group similarityTo" should {
    "match" in {
      game1.similarityTo(game2) must_=={MatchAndSig(true, 1.0)}
    }
  }

  "Group compareMoveTimes" should {
    "match" in {
      game1.compareMoveTimes(game2).isSimilar must_=={true}
      game1.compareMoveTimes(game2).apply must_=={1.0}
    }
  }

  "Group compareSfAccuracies" should {
    "match" in {
      game1.compareSfAccuracies(game2).isSimilar must_=={true}
      game1.compareSfAccuracies(game2).apply must_=={1.0}
    }
  }

  "Group compareBlurRates" should {
    "match" in {
      game1.compareBlurRates(game2).isSimilar must_=={true}
      game1.compareBlurRates(game2).apply must_=={1.0}
    }
  }

  "Group compareHoldAlerts" should {
    "match" in {
      game1.compareHoldAlerts(game2).isSimilar must_=={true}
      game1.compareHoldAlerts(game2).apply must_=={1.0}
    }
  }
}