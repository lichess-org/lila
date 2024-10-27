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
          case Glicko.Result.Win  => GameResult(wRating, bRating, Some(true))
          case Glicko.Result.Loss => GameResult(wRating, bRating, Some(false))
          case Glicko.Result.Draw => GameResult(wRating, bRating, None)
      )
    )
    Glicko.calculator(35.0d).updateRatings(results, true)

  test("default deviation: white wins") {
    val wr = default.toRating
    val br = default.toRating
    updateRatings(wr, br, White.some)
    assertCloseTo(wr.rating, 1729d, 1d)
    assertCloseTo(br.rating, 1271d, 1d)
    assertCloseTo(wr.ratingDeviation, 396.9015d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 396.9015d, 0.0001d)
    assertCloseTo(wr.volatility, 0.0899980, 0.0000001d)
    assertCloseTo(br.volatility, 0.0899980, 0.0000001d)
  }
  test("default deviation: black wins") {
    val wr = default.toRating
    val br = default.toRating
    updateRatings(wr, br, Black.some)
    assertCloseTo(wr.rating, 1245d, 1d)
    assertCloseTo(br.rating, 1755d, 1d)
    assertCloseTo(wr.ratingDeviation, 396.9015d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 396.9015d, 0.0001d)
    assertCloseTo(wr.volatility, 0.0899986, 0.0000001d)
    assertCloseTo(br.volatility, 0.0899986, 0.0000001d)
  }
  test("default deviation: draw") {
    val wr = default.toRating
    val br = default.toRating
    updateRatings(wr, br, None)
    assertCloseTo(wr.rating, 1487d, 1d)
    assertCloseTo(br.rating, 1513d, 1d)
    assertCloseTo(wr.ratingDeviation, 396.9015d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 396.9015d, 0.0001d)
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
    assertCloseTo(wr.rating, 1515d, 1d)
    assertCloseTo(br.rating, 1485d, 1d)
    assertCloseTo(wr.ratingDeviation, 78.0967d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 78.0967d, 0.0001d)
    assertCloseTo(wr.volatility, 0.06, 0.00001d)
    assertCloseTo(br.volatility, 0.06, 0.00001d)
  }
  test("low deviation: black wins") {
    val wr = perf.toRating
    val br = perf.toRating
    updateRatings(wr, br, Black.some)
    assertCloseTo(wr.rating, 1481d, 1d)
    assertCloseTo(br.rating, 1519d, 1d)
    assertCloseTo(wr.ratingDeviation, 78.0967d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 78.0967d, 0.0001d)
    assertCloseTo(wr.volatility, 0.06, 0.00001d)
    assertCloseTo(br.volatility, 0.06, 0.00001d)
  }
  test("low deviation: draw") {
    val wr = perf.toRating
    val br = perf.toRating
    updateRatings(wr, br, None)
    assertCloseTo(wr.rating, 1498d, 1d)
    assertCloseTo(br.rating, 1502d, 1d)
    assertCloseTo(wr.ratingDeviation, 78.0967d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 78.0967d, 0.0001d)
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
      assertCloseTo(br.rating, 1509d, 1d)
      assertCloseTo(wr.ratingDeviation, 77.3966, 0.0001d)
      assertCloseTo(br.ratingDeviation, 105.5926, 0.0001d)
      assertCloseTo(wr.volatility, 0.06, 0.00001d)
      assertCloseTo(br.volatility, 0.065, 0.00001d)
    }
    test("mixed ratings and deviations: black wins") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, Black.some)
      assertCloseTo(wr.rating, 1389d, 1d)
      assertCloseTo(br.rating, 1571d, 1d)
      assertCloseTo(wr.ratingDeviation, 77.3966, 0.0001d)
      assertCloseTo(br.ratingDeviation, 105.5926, 0.0001d)
      assertCloseTo(wr.volatility, 0.06, 0.00001d)
      assertCloseTo(br.volatility, 0.065, 0.00001d)
    }
    test("mixed ratings and deviations: draw") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, None)
      assertCloseTo(wr.rating, 1405d, 1d)
      assertCloseTo(br.rating, 1540d, 1d)
      assertCloseTo(wr.ratingDeviation, 77.3966, 0.0001d)
      assertCloseTo(br.ratingDeviation, 105.5926, 0.0001d)
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
      assertCloseTo(wr.rating, 1216.6d, 0.1d)
      assertCloseTo(br.rating, 1638.4d, 0.1d)
      assertCloseTo(wr.ratingDeviation, 59.8839d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 196.3840, 0.0001d)
      assertCloseTo(wr.volatility, 0.053013, 0.000001d)
      assertCloseTo(br.volatility, 0.062028, 0.000001d)
    }
    test("more mixed ratings and deviations: black wins") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, Black.some)
      assertCloseTo(wr.rating, 1199.2d, 0.1d)
      assertCloseTo(br.rating, 1856.5d, 0.1d)
      assertCloseTo(wr.ratingDeviation, 59.8839d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 196.3840, 0.0001d)
      assertCloseTo(wr.volatility, 0.052999, 0.000001d)
      assertCloseTo(br.volatility, 0.061999, 0.000001d)
    }
    test("more mixed ratings and deviations: draw") {
      val wr = wP.toRating
      val br = bP.toRating
      updateRatings(wr, br, None)
      assertCloseTo(wr.rating, 1207.9, 0.1d)
      assertCloseTo(br.rating, 1747.5, 0.1d)
      assertCloseTo(wr.ratingDeviation, 59.8839, 0.0001d)
      assertCloseTo(br.ratingDeviation, 196.3840, 0.0001d)
      assertCloseTo(wr.volatility, 0.053002, 0.000001d)
      assertCloseTo(br.volatility, 0.062006, 0.000001d)
    }
  }
