package lila.rating

import chess.{ Black, Color, White }

import lila.rating.Perf.default
import lila.rating.PerfExt.*

import glicko2.*

class RatingCalculatorTest extends lila.common.LilaTest:
  def updateRatings(wRating: Rating, bRating: Rating, winner: Option[Color]) =
    val ratings = Set(wRating, bRating)
    val result = winner match
      case Some(chess.White) => Glicko.Result.Win
      case Some(chess.Black) => Glicko.Result.Loss
      case None              => Glicko.Result.Draw
    val results = GameRatingPeriodResults(
      List(
        result match
          case Glicko.Result.Win  => GameResult(wRating, bRating, Some(true))
          case Glicko.Result.Loss => GameResult(wRating, bRating, Some(false))
          case Glicko.Result.Draw => GameResult(wRating, bRating, None)
      )
    )
    Glicko.calculator(ColorAdvantage.standard).updateRatings(ratings, results, true)

  test("default deviation: white wins") {
    val wr = default.toRating
    val br = default.toRating
    updateRatings(wr, br, White.some)
    assertCloseTo(wr.rating, 1739d, 0.5d)
    assertCloseTo(br.rating, 1261d, 0.5d)
    assertCloseTo(wr.ratingDeviation, 396.7003d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 396.7003d, 0.0001d)
    assertCloseTo(wr.volatility, 0.09000d, 0.00001d)
    assertCloseTo(br.volatility, 0.09000d, 0.00001d)
  }
  test("default deviation: black wins") {
    val wr = default.toRating
    val br = default.toRating
    updateRatings(wr, br, Black.some)
    assertCloseTo(wr.rating, 1256d, 0.5d)
    assertCloseTo(br.rating, 1744d, 0.5d)
    assertCloseTo(wr.ratingDeviation, 396.7003d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 396.7003d, 0.0001d)
    assertCloseTo(wr.volatility, 0.09000d, 0.00001d)
    assertCloseTo(br.volatility, 0.09000d, 0.00001d)
  }
  test("default deviation: draw") {
    val wr = default.toRating
    val br = default.toRating
    updateRatings(wr, br, None)
    assertCloseTo(wr.rating, 1497d, 0.5d)
    assertCloseTo(br.rating, 1503d, 0.5d)
    assertCloseTo(wr.ratingDeviation, 396.7003d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 396.7003d, 0.0001d)
    assertCloseTo(wr.volatility, 0.09000d, 0.00001d)
    assertCloseTo(br.volatility, 0.09000d, 0.00001d)
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
    assertCloseTo(wr.rating, 1517d, 0.5d)
    assertCloseTo(br.rating, 1483d, 0.5d)
    assertCloseTo(wr.ratingDeviation, 78.0800d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 78.0800d, 0.0001d)
    assertCloseTo(wr.volatility, 0.06000d, 0.00001d)
    assertCloseTo(br.volatility, 0.06000d, 0.00001d)
  }
  test("low deviation: black wins") {
    val wr = perf.toRating
    val br = perf.toRating
    updateRatings(wr, br, Black.some)
    assertCloseTo(wr.rating, 1483d, 0.5d)
    assertCloseTo(br.rating, 1517d, 0.5d)
    assertCloseTo(wr.ratingDeviation, 78.0800d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 78.0800d, 0.0001d)
    assertCloseTo(wr.volatility, 0.06000d, 0.00001d)
    assertCloseTo(br.volatility, 0.06000d, 0.00001d)
  }
  test("low deviation: draw") {
    val wr = perf.toRating
    val br = perf.toRating
    updateRatings(wr, br, None)
    assertCloseTo(wr.rating, 1500d, 0.5d)
    assertCloseTo(br.rating, 1500d, 0.5d)
    assertCloseTo(wr.ratingDeviation, 78.0800d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 78.0800d, 0.0001d)
    assertCloseTo(wr.volatility, 0.06000d, 0.00001d)
    assertCloseTo(br.volatility, 0.06000d, 0.00001d)
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
      assertCloseTo(wr.rating, 1422d, 0.5d)
      assertCloseTo(br.rating, 1507d, 0.5d)
      assertCloseTo(wr.ratingDeviation, 77.4720d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 105.8046d, 0.0001d)
      assertCloseTo(wr.volatility, 0.06000d, 0.00001d)
      assertCloseTo(br.volatility, 0.06500d, 0.00001d)
    }
    test("mixed ratings and deviations: black wins") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, Black.some)
      assertCloseTo(wr.rating, 1390d, 0.5d)
      assertCloseTo(br.rating, 1569d, 0.5d)
      assertCloseTo(wr.ratingDeviation, 77.4720d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 105.8046d, 0.0001d)
      assertCloseTo(wr.volatility, 0.06000d, 0.00001d)
      assertCloseTo(br.volatility, 0.06500d, 0.00001d)
    }
    test("mixed ratings and deviations: draw") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, None)
      assertCloseTo(wr.rating, 1406d, 0.5d)
      assertCloseTo(br.rating, 1538d, 0.5d)
      assertCloseTo(wr.ratingDeviation, 77.4720d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 105.8046d, 0.0001d)
      assertCloseTo(wr.volatility, 0.06000d, 0.00001d)
      assertCloseTo(br.volatility, 0.06500d, 0.00001d)
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
      assertCloseTo(wr.rating, 1217d, 0.5d)
      assertCloseTo(br.rating, 1637d, 0.5d)
      assertCloseTo(wr.ratingDeviation, 59.8971d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 196.8617d, 0.0001d)
      assertCloseTo(wr.volatility, 0.053013d, 0.00001d)
      assertCloseTo(br.volatility, 0.062028d, 0.00001d)
    }
    test("more mixed ratings and deviations: black wins") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, Black.some)
      assertCloseTo(wr.rating, 1199d, 0.5d)
      assertCloseTo(br.rating, 1856d, 0.5d)
      assertCloseTo(wr.ratingDeviation, 59.8971d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 196.8617d, 0.0001d)
      assertCloseTo(wr.volatility, 0.052999d, 0.00001d)
      assertCloseTo(br.volatility, 0.061999d, 0.00001d)
    }
    test("more mixed ratings and deviations: draw") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, None)
      assertCloseTo(wr.rating, 1208d, 0.5d)
      assertCloseTo(br.rating, 1746d, 0.5d)
      assertCloseTo(wr.ratingDeviation, 59.8971d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 196.8617d, 0.0001d)
      assertCloseTo(wr.volatility, 0.052999d, 0.00001d)
      assertCloseTo(br.volatility, 0.062006d, 0.00001d)
    }
  }
