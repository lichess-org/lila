package lila.evaluation
package grouping

import org.specs2.specification._
import org.specs2.mutable._

import scalaz.NonEmptyList

class GroupTest extends Specification {
  val game1 = GameGroup(
    NonEmptyList(15, 40, 10, 30, 20, 40, 20, 80, 30, 30, 10, 40, 20, 60, 100, 80, 50, 30, 50, 5, 100, 300, 150, 50, 60, 20, 15, 15, 15, 60, 30, 80, 80, 150, 80, 200, 10, 80, 60, 150, 50, 30, 30, 150, 40, 40, 30, 30),
    NonEmptyList(0, 1, 2, 1),
    List(3),
    List()
  )
  val game2 = GameGroup(
    NonEmptyList(1, 20, 20, 10, 15, 30, 1, 10, 20, 40, 20, 5, 15, 400, 60, 60, 80, 100, 80, 400, 400, 20, 60, 200, 20, 30, 5, 100, 40, 30, 20, 5, 30, 300, 50, 80, 30, 100, 40),
    NonEmptyList(42, 94, 111, 0, 9, 26, 2, 84, 0, 225, 11, 0, 32, 0, 0, 0, 6, 0, 9),
    List(5),
    List()
  )
  val game3 = GameGroup(
    NonEmptyList(1, 60, 600, 150, 600, 600, 600, 300, 600, 150, 100, 600, 600, 400, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600),
    NonEmptyList(6, 286, 54, 14, 0, 0, 6, 0, 4, 29, 1, 0, 17, 0, 0, 6, 4, 22),
    List(14),
    List()
  )
  val game4 = GameGroup(
    NonEmptyList(1, 5, 80, 200, 20, 1, 40, 10, 20, 10, 100, 40, 40, 40, 40, 30, 60, 20, 30, 20, 40, 20, 50, 20, 100, 50, 100, 30, 150, 60, 80, 20, 30, 30, 60, 100, 400, 100, 150, 150, 600, 100, 150, 200, 150, 30, 20, 150, 300, 30, 80, 50, 60, 40, 100, 150, 150, 20, 50, 50, 40, 200, 30, 30, 5, 50, 150, 60, 20, 50, 40, 150, 40, 30, 50, 150, 20, 100, 30, 100, 20, 40, 50, 50, 40, 80, 30, 30, 20, 10, 15, 20, 15, 15, 40, 15, 15, 15, 80, 30, 60, 15, 20, 15, 30, 20, 20, 20, 10, 30, 15, 15, 40, 15, 10, 15, 20, 20, 30, 20, 30, 20, 15, 15, 30, 20, 30, 30, 20, 20, 15, 15, 15, 60, 20, 15, 30, 30, 20, 20, 20, 50, 20, 30, 80, 100, 30, 50, 15, 15, 40, 20, 15),
    NonEmptyList(0, 0, 0, 0, 0, 0, 0, 0, 994, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 335, 0, 0, 0, 0, 74, 2, 145, 189, 0, 40, 3, 67, 0, 46, 0, 677, 13, 0, 14, 36, 18, 117, 58, 89, 0, 0, 9, 10, 50, 0, 0, 0, 2, 52, 13, 113, 79, 37, 0, 50, 0, 28, 22, 46, 19, 0, 0, 26, 24),
    List(2),
    List()
  )

  "Group similarityTo" should {
    "Game1 Match Game1" in {
      game1.similarityTo(game1) must_=={MatchAndSig(true, 1.0)}
    }
    "Game1 Match Game2" in {
      game1.similarityTo(game2) must_=={MatchAndSig(false, 1.0)}
    }
    "Game1 Match Game3" in {
      game1.similarityTo(game3) must_=={MatchAndSig(false, 1.0)}
    }
    "Game1 Match Game4" in {
      game1.similarityTo(game4) must_=={MatchAndSig(false, 1.0)}
    }

    "Game2 Match Game3" in {
      game2.similarityTo(game3) must_=={MatchAndSig(false, 1.0)}
    }
    "Game2 Match Game4" in {
      game2.similarityTo(game4) must_=={MatchAndSig(false, 1.0)}
    }

    "Game3 Match Game4" in {
      game3.similarityTo(game4) must_=={MatchAndSig(false, 1.0)}
    }
  }

  "Group compareMoveTimes" should {
    "Game1 Match Game1" in {
      game1.compareMoveTimes(game1).isSimilar must_=={true}
      game1.compareMoveTimes(game1).apply must_=={1.0}
    }
    "Game1 Match Game2" in {
      game1.compareMoveTimes(game2).isSimilar must_=={true}
      game1.compareMoveTimes(game2).apply must_=={1.0}
    }
    "Game1 Match Game3" in {
      game1.compareMoveTimes(game3).isSimilar must_=={true}
      game1.compareMoveTimes(game3).apply must_=={1.0}
    }
    "Game1 Match Game4" in {
      game1.compareMoveTimes(game4).isSimilar must_=={true}
      game1.compareMoveTimes(game4).apply must_=={1.0}
    }

    "Game2 Match Game3" in {
      game2.compareMoveTimes(game3).isSimilar must_=={true}
      game2.compareMoveTimes(game3).apply must_=={1.0}
    }
    "Game2 Match Game4" in {
      game2.compareMoveTimes(game4).isSimilar must_=={true}
      game2.compareMoveTimes(game4).apply must_=={1.0}
    }

    "Game3 Match Game4" in {
      game3.compareMoveTimes(game4).isSimilar must_=={true}
      game3.compareMoveTimes(game4).apply must_=={1.0}
    }
  }

  "Group compareSfAccuracies" should {
    "Game1 Match Game1" in {
      game1.compareSfAccuracies(game1).isSimilar must_=={true}
      game1.compareSfAccuracies(game1).apply must_=={1.0}
    }
    "Game1 Match Game2" in {
      game1.compareSfAccuracies(game2).isSimilar must_=={true}
      game1.compareSfAccuracies(game2).apply must_=={1.0}
    }
    "Game1 Match Game3" in {
      game1.compareSfAccuracies(game3).isSimilar must_=={true}
      game1.compareSfAccuracies(game4).apply must_=={1.0}
    }
    "Game1 Match Game4" in {
      game1.compareSfAccuracies(game4).isSimilar must_=={true}
      game1.compareSfAccuracies(game4).apply must_=={1.0}
    }

    "Game2 Match Game3" in {
      game2.compareSfAccuracies(game3).isSimilar must_=={true}
      game2.compareSfAccuracies(game3).apply must_=={1.0}
    }
    "Game2 Match Game4" in {
      game2.compareSfAccuracies(game4).isSimilar must_=={true}
      game2.compareSfAccuracies(game4).apply must_=={1.0}
    }

    "Game3 Match Game4" in {
      game3.compareSfAccuracies(game4).isSimilar must_=={true}
      game3.compareSfAccuracies(game4).apply must_=={1.0}
    }
  }

  "Group compareBlurRates" should {
    "Game1 Match Game1" in {
      game1.compareBlurRates(game1).isSimilar must_=={true}
      game1.compareBlurRates(game1).apply must_=={1.0}
    }
    "Game1 Match Game2" in {
      game1.compareBlurRates(game2).isSimilar must_=={true}
      game1.compareBlurRates(game2).apply must_=={1.0}
    }
    "Game1 Match Game3" in {
      game1.compareBlurRates(game3).isSimilar must_=={true}
      game1.compareBlurRates(game3).apply must_=={1.0}
    }
    "Game1 Match Game4" in {
      game1.compareBlurRates(game4).isSimilar must_=={true}
      game1.compareBlurRates(game4).apply must_=={1.0}
    }

    "Game2 Match Game3" in {
      game2.compareBlurRates(game3).isSimilar must_=={true}
      game2.compareBlurRates(game3).apply must_=={1.0}
    }
    "Game2 Match Game4" in {
      game2.compareBlurRates(game4).isSimilar must_=={true}
      game2.compareBlurRates(game4).apply must_=={1.0}
    }

    "Game3 Match Game4" in {
      game3.compareBlurRates(game4).isSimilar must_=={true}
      game3.compareBlurRates(game4).apply must_=={1.0}
    }
  }

  "Group compareHoldAlerts" should {
    "Game1 Match Game2" in {
      game1.compareHoldAlerts(game2).isSimilar must_=={true}
      game1.compareHoldAlerts(game2).apply must_=={1.0}
    }
  }
}
