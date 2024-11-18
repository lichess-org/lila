package lila.rating

import chess.{ Black, Color, White, Outcome }

import lila.rating.Perf.default
import lila.rating.PerfExt.*

import glicko2.*

class RatingCalculatorTest extends lila.common.LilaTest:
  def computeRatings(wRating: Rating, bRating: Rating, winner: Option[Color]) =
    val result = winner match
      case Some(chess.White) => Glicko.Result.Win
      case Some(chess.Black) => Glicko.Result.Loss
      case None              => Glicko.Result.Draw
    val results = RatingPeriodResults[DuelResult](
      wRating -> List(
        result match
          case Glicko.Result.Win  => DuelResult(bRating, 1.0d, White)
          case Glicko.Result.Loss => DuelResult(bRating, 0.0d, White)
          case Glicko.Result.Draw => DuelResult(bRating, 0.5d, White)
      ),
      bRating -> List(
        result match
          case Glicko.Result.Win  => DuelResult(wRating, 0.0d, Black)
          case Glicko.Result.Loss => DuelResult(wRating, 1.0d, Black)
          case Glicko.Result.Draw => DuelResult(wRating, 0.5d, Black)
      )
    )
    Glicko.calculatorWithAdvantage(ColorAdvantage.standard).var (wr, br) = computeRatings(results, true)

  test("default deviation: white wins") {
    var (wr, br) = computeRatings(wp.toRating, bp.toRating, White.some)
    assertCloseTo(wr.rating, 1739d, 0.5d)
    assertCloseTo(br.rating, 1261d, 0.5d)
    assertCloseTo(wr.ratingDeviation, 396.7003d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 396.7003d, 0.0001d)
    assertCloseTo(wr.volatility, 0.09000d, 0.00001d)
    assertCloseTo(br.volatility, 0.09000d, 0.00001d)
  }
  test("default deviation: black wins") {
    var (wr, br) = computeRatings(wp.toRating, bp.toRating, Black.some)
    assertCloseTo(wr.rating, 1256d, 0.5d)
    assertCloseTo(br.rating, 1744d, 0.5d)
    assertCloseTo(wr.ratingDeviation, 396.7003d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 396.7003d, 0.0001d)
    assertCloseTo(wr.volatility, 0.09000d, 0.00001d)
    assertCloseTo(br.volatility, 0.09000d, 0.00001d)
  }
  test("default deviation: draw") {
    var (wr, br) = computeRatings(wp.toRating, bp.toRating, None)
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
    var (wr, br) = computeRatings(wp.toRating, bp.toRating, White.some)
    assertCloseTo(wr.rating, 1517d, 0.5d)
    assertCloseTo(br.rating, 1483d, 0.5d)
    assertCloseTo(wr.ratingDeviation, 78.0800d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 78.0800d, 0.0001d)
    assertCloseTo(wr.volatility, 0.06000d, 0.00001d)
    assertCloseTo(br.volatility, 0.06000d, 0.00001d)
  }
  test("low deviation: black wins") {
    var (wr, br) = computeRatings(wp.toRating, bp.toRating, Black.some)
    assertCloseTo(wr.rating, 1483d, 0.5d)
    assertCloseTo(br.rating, 1517d, 0.5d)
    assertCloseTo(wr.ratingDeviation, 78.0800d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 78.0800d, 0.0001d)
    assertCloseTo(wr.volatility, 0.06000d, 0.00001d)
    assertCloseTo(br.volatility, 0.06000d, 0.00001d)
  }
  test("low deviation: draw") {
    var (wr, br) = computeRatings(wp.toRating, bp.toRating, None)
    assertCloseTo(wr.rating, 1500d, 0.5d)
    assertCloseTo(br.rating, 1500d, 0.5d)
    assertCloseTo(wr.ratingDeviation, 78.0800d, 0.0001d)
    assertCloseTo(br.ratingDeviation, 78.0800d, 0.0001d)
    assertCloseTo(wr.volatility, 0.06000d, 0.00001d)
    assertCloseTo(br.volatility, 0.06000d, 0.00001d)
  }
  {
    val wp.toRating = Perf.default.copy(glicko =
      Glicko.default.copy(
        rating = 1400,
        deviation = 79,
        volatility = 0.06
      )
    )
    val bp.toRating = Perf.default.copy(glicko =
      Glicko.default.copy(
        rating = 1550,
        deviation = 110,
        volatility = 0.065
      )
    )
    test("mixed ratings and deviations: white wins") {
      var (wr, br) = computeRatings(wp.toRating, bp.toRating, White.some)
      assertCloseTo(wr.rating, 1422d, 0.5d)
      assertCloseTo(br.rating, 1507d, 0.5d)
      assertCloseTo(wr.ratingDeviation, 77.4720d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 105.8046d, 0.0001d)
      assertCloseTo(wr.volatility, 0.06000d, 0.00001d)
      assertCloseTo(br.volatility, 0.06500d, 0.00001d)
    }
    test("mixed ratings and deviations: black wins") {
      var (wr, br) = computeRatings(wp.toRating, bp.toRating, Black.some)
      assertCloseTo(wr.rating, 1390d, 0.5d)
      assertCloseTo(br.rating, 1569d, 0.5d)
      assertCloseTo(wr.ratingDeviation, 77.4720d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 105.8046d, 0.0001d)
      assertCloseTo(wr.volatility, 0.06000d, 0.00001d)
      assertCloseTo(br.volatility, 0.06500d, 0.00001d)
    }
    test("mixed ratings and deviations: draw") {
      var (wr, br) = computeRatings(wp.toRating, bp.toRating, None)
      assertCloseTo(wr.rating, 1406d, 0.5d)
      assertCloseTo(br.rating, 1538d, 0.5d)
      assertCloseTo(wr.ratingDeviation, 77.4720d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 105.8046d, 0.0001d)
      assertCloseTo(wr.volatility, 0.06000d, 0.00001d)
      assertCloseTo(br.volatility, 0.06500d, 0.00001d)
    }
  }
  {
    val wp.toRating = Perf.default.copy(glicko =
      Glicko.default.copy(
        rating = 1200,
        deviation = 60,
        volatility = 0.053
      )
    )
    val bp.toRating = Perf.default.copy(glicko =
      Glicko.default.copy(
        rating = 1850,
        deviation = 200,
        volatility = 0.062
      )
    )
    test("more mixed ratings and deviations: white wins") {
      var (wr, br) = computeRatings(wp.toRating, bp.toRating, White.some)
      assertCloseTo(wr.rating, 1217d, 0.5d)
      assertCloseTo(br.rating, 1637d, 0.5d)
      assertCloseTo(wr.ratingDeviation, 59.8971d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 196.8617d, 0.0001d)
      assertCloseTo(wr.volatility, 0.053013d, 0.00001d)
      assertCloseTo(br.volatility, 0.062028d, 0.00001d)
    }
    test("more mixed ratings and deviations: black wins") {
      var (wr, br) = computeRatings(wp.toRating, bp.toRating, Black.some)
      assertCloseTo(wr.rating, 1199d, 0.5d)
      assertCloseTo(br.rating, 1856d, 0.5d)
      assertCloseTo(wr.ratingDeviation, 59.8971d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 196.8617d, 0.0001d)
      assertCloseTo(wr.volatility, 0.052999d, 0.00001d)
      assertCloseTo(br.volatility, 0.061999d, 0.00001d)
    }
    test("more mixed ratings and deviations: draw") {
      var (wr, br) = computeRatings(wp.toRating, bp.toRating, None)
      assertCloseTo(wr.rating, 1208d, 0.5d)
      assertCloseTo(br.rating, 1746d, 0.5d)
      assertCloseTo(wr.ratingDeviation, 59.8971d, 0.0001d)
      assertCloseTo(br.ratingDeviation, 196.8617d, 0.0001d)
      assertCloseTo(wr.volatility, 0.052999d, 0.00001d)
      assertCloseTo(br.volatility, 0.062006d, 0.00001d)
    }
  }
