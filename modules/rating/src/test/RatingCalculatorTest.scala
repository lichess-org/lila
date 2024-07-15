package lila.rating

import chess.{ Black, Color, White }

import lila.rating.Perf.default
import lila.rating.PerfExt.*

import glicko2.*

class RatingCalculatorTest extends lila.common.LilaTest:

  def updateRatings(wRating: Rating, bRating: Rating, winner: Option[Color]) =
    val result = winner match
      case Some(chess.White) => Glicko.Result.Win
      case Some(chess.Black) => Glicko.Result.Loss
      case None              => Glicko.Result.Draw
    val results = GameRatingPeriodResults(
      List(
        result match
          case Glicko.Result.Draw => GameResult(wRating, bRating, true)
          case Glicko.Result.Win  => GameResult(wRating, bRating, false)
          case Glicko.Result.Loss => GameResult(bRating, wRating, false)
      )
    )
    Glicko.system.updateRatings(results, true)

  test("default deviation: white wins") {
    val wr = default.toRating
    val br = default.toRating
    updateRatings(wr, br, White.some)
    assertCloseTo(wr.rating, 1741d, 1d)
    assertCloseTo(br.rating, 1258d, 1d)
    assertCloseTo(wr.ratingDeviation, 396d, 1d)
    assertCloseTo(br.ratingDeviation, 396d, 1d)
    assertCloseTo(wr.volatility, 0.0899983, 0.00000001d)
    assertCloseTo(br.volatility, 0.0899983, 0.0000001d)
  }
  test("default deviation: black wins") {
    val wr = default.toRating
    val br = default.toRating
    updateRatings(wr, br, Black.some)
    assertCloseTo(wr.rating, 1258d, 1d)
    assertCloseTo(br.rating, 1741d, 1d)
    assertCloseTo(wr.ratingDeviation, 396d, 1d)
    assertCloseTo(br.ratingDeviation, 396d, 1d)
    assertCloseTo(wr.volatility, 0.0899983, 0.00000001d)
    assertCloseTo(br.volatility, 0.0899983, 0.0000001d)
  }
  test("default deviation: draw") {
    val wr = default.toRating
    val br = default.toRating
    updateRatings(wr, br, None)
    assertCloseTo(wr.rating, 1500d, 1d)
    assertCloseTo(br.rating, 1500d, 1d)
    assertCloseTo(wr.ratingDeviation, 396d, 1d)
    assertCloseTo(br.ratingDeviation, 396d, 1d)
    assertCloseTo(wr.volatility, 0.0899954, 0.0000001d)
    assertCloseTo(br.volatility, 0.0899954, 0.0000001d)
  }
  val perf = Perf.default.copy(glicko =
    Glicko.default.copy(
      deviation = 80,
      volatility = 0.06
    )
  )
  test("low deviation: white wins") {
    val wr = perf.toRating
    val br = perf.toRating
    updateRatings(wr, br, White.some)
    assertCloseTo(wr.rating, 1517d, 1d)
    assertCloseTo(br.rating, 1482d, 1d)
    assertCloseTo(wr.ratingDeviation, 78d, 1d)
    assertCloseTo(br.ratingDeviation, 78d, 1d)
    assertCloseTo(wr.volatility, 0.06, 0.00001d)
    assertCloseTo(br.volatility, 0.06, 0.00001d)
  }
  test("low deviation: black wins") {
    val wr = perf.toRating
    val br = perf.toRating
    updateRatings(wr, br, Black.some)
    assertCloseTo(wr.rating, 1482d, 1d)
    assertCloseTo(br.rating, 1517d, 1d)
    assertCloseTo(wr.ratingDeviation, 78d, 1d)
    assertCloseTo(br.ratingDeviation, 78d, 1d)
    assertCloseTo(wr.volatility, 0.06, 0.00001d)
    assertCloseTo(br.volatility, 0.06, 0.00001d)
  }
  test("low deviation: draw") {
    val wr = perf.toRating
    val br = perf.toRating
    updateRatings(wr, br, None)
    assertCloseTo(wr.rating, 1500d, 1d)
    assertCloseTo(br.rating, 1500d, 1d)
    assertCloseTo(wr.ratingDeviation, 78d, 1d)
    assertCloseTo(br.ratingDeviation, 78d, 1d)
    assertCloseTo(wr.volatility, 0.06, 0.00001d)
    assertCloseTo(br.volatility, 0.06, 0.00001d)
  }
  {
    val wP = Perf.default.copy(glicko =
      Glicko.default.copy(
        rating = 1400,
        deviation = 79,
        volatility = 0.06
      )
    )
    val bP = Perf.default.copy(glicko =
      Glicko.default.copy(
        rating = 1550,
        deviation = 110,
        volatility = 0.065
      )
    )
    test("mixed ratings and deviations: white wins") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, White.some)
      assertCloseTo(wr.rating, 1422d, 1d)
      assertCloseTo(br.rating, 1506d, 1d)
      assertCloseTo(wr.ratingDeviation, 77d, 1d)
      assertCloseTo(br.ratingDeviation, 105d, 1d)
      assertCloseTo(wr.volatility, 0.06, 0.00001d)
      assertCloseTo(br.volatility, 0.065, 0.00001d)
    }
    test("mixed ratings and deviations: black wins") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, Black.some)
      assertCloseTo(wr.rating, 1389d, 1d)
      assertCloseTo(br.rating, 1568d, 1d)
      assertCloseTo(wr.ratingDeviation, 78d, 1d)
      assertCloseTo(br.ratingDeviation, 105d, 1d)
      assertCloseTo(wr.volatility, 0.06, 0.00001d)
      assertCloseTo(br.volatility, 0.065, 0.00001d)
    }
    test("mixed ratings and deviations: draw") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, None)
      assertCloseTo(wr.rating, 1406d, 1d)
      assertCloseTo(br.rating, 1537d, 1d)
      assertCloseTo(wr.ratingDeviation, 78d, 1d)
      assertCloseTo(br.ratingDeviation, 105.87d, 0.01d)
      assertCloseTo(wr.volatility, 0.06, 0.00001d)
      assertCloseTo(br.volatility, 0.065, 0.00001d)
    }
  }
  {
    val wP = Perf.default.copy(glicko =
      Glicko.default.copy(
        rating = 1200,
        deviation = 60,
        volatility = 0.053
      )
    )
    val bP = Perf.default.copy(glicko =
      Glicko.default.copy(
        rating = 1850,
        deviation = 200,
        volatility = 0.062
      )
    )
    test("more mixed ratings and deviations: white wins") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, White.some)
      assertCloseTo(wr.rating, 1216.7d, 0.1d)
      assertCloseTo(br.rating, 1636d, 0.1d)
      assertCloseTo(wr.ratingDeviation, 59.9d, 0.1d)
      assertCloseTo(br.ratingDeviation, 196.9d, 0.1d)
      assertCloseTo(wr.volatility, 0.053013, 0.000001d)
      assertCloseTo(br.volatility, 0.062028, 0.000001d)
    }
    test("more mixed ratings and deviations: black wins") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, Black.some)
      assertCloseTo(wr.rating, 1199.3d, 0.1d)
      assertCloseTo(br.rating, 1855.4d, 0.1d)
      assertCloseTo(wr.ratingDeviation, 59.9d, 0.1d)
      assertCloseTo(br.ratingDeviation, 196.9d, 0.1d)
      assertCloseTo(wr.volatility, 0.052999, 0.000001d)
      assertCloseTo(br.volatility, 0.061999, 0.000001d)
    }
    test("more mixed ratings and deviations: draw") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, None)
      assertCloseTo(wr.rating, 1208.0, 0.1d)
      assertCloseTo(br.rating, 1745.7, 0.1d)
      assertCloseTo(wr.ratingDeviation, 59.90056, 0.1d)
      assertCloseTo(br.ratingDeviation, 196.98729, 0.1d)
      assertCloseTo(wr.volatility, 0.053002, 0.000001d)
      assertCloseTo(br.volatility, 0.062006, 0.000001d)
    }
  }
