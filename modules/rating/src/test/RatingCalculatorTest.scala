package lila.rating

import org.specs2.mutable.Specification
import org.goochjs.glicko2.*
import chess.{ Color, White, Black }

class RatingCalculatorTest extends Specification {

  def updateRatings(wRating: Rating, bRating: Rating, winner: Option[Color]) =
    val result = winner match
      case Some(chess.White) => Glicko.Result.Win
      case Some(chess.Black) => Glicko.Result.Loss
      case None              => Glicko.Result.Draw
    val results = new GameRatingPeriodResults()
    result match
      case Glicko.Result.Draw => results.addDraw(wRating, bRating)
      case Glicko.Result.Win  => results.addWin(wRating, bRating)
      case Glicko.Result.Loss => results.addWin(bRating, wRating)
    Glicko.system.updateRatings(results, true)

  "compute rating" in {
    "default deviation" in {
      "white wins" in {
        val wr = Perf.default.toRating
        val br = Perf.default.toRating
        updateRatings(wr, br, White.some)
        wr.getRating must beCloseTo(1741d, 1d)
        br.getRating must beCloseTo(1258d, 1d)
        wr.getRatingDeviation must beCloseTo(396d, 1d)
        br.getRatingDeviation must beCloseTo(396d, 1d)
        wr.getVolatility must beCloseTo(0.08999, 0.00001d)
        br.getVolatility must beCloseTo(0.08999, 0.00001d)
      }
      "black wins" in {
        val wr = Perf.default.toRating
        val br = Perf.default.toRating
        updateRatings(wr, br, Black.some)
        wr.getRating must beCloseTo(1258d, 1d)
        br.getRating must beCloseTo(1741d, 1d)
        wr.getRatingDeviation must beCloseTo(396d, 1d)
        br.getRatingDeviation must beCloseTo(396d, 1d)
        wr.getVolatility must beCloseTo(0.08999, 0.00001d)
        br.getVolatility must beCloseTo(0.08999, 0.00001d)
      }
      "draw" in {
        val wr = Perf.default.toRating
        val br = Perf.default.toRating
        updateRatings(wr, br, None)
        wr.getRating must beCloseTo(1500d, 1d)
        br.getRating must beCloseTo(1500d, 1d)
        wr.getRatingDeviation must beCloseTo(396d, 1d)
        br.getRatingDeviation must beCloseTo(396d, 1d)
        wr.getVolatility must beCloseTo(0.08999, 0.00001d)
        br.getVolatility must beCloseTo(0.08999, 0.00001d)
      }
    }
    "low deviation" in {
      val perf = Perf.default.copy(glicko =
        Glicko.default.copy(
          deviation = 80,
          volatility = 0.06
        )
      )
      "white wins" in {
        val wr = perf.toRating
        val br = perf.toRating
        updateRatings(wr, br, White.some)
        wr.getRating must beCloseTo(1517d, 1d)
        br.getRating must beCloseTo(1482d, 1d)
        wr.getRatingDeviation must beCloseTo(78d, 1d)
        br.getRatingDeviation must beCloseTo(78d, 1d)
        wr.getVolatility must beCloseTo(0.06, 0.00001d)
        br.getVolatility must beCloseTo(0.06, 0.00001d)
      }
      "black wins" in {
        val wr = perf.toRating
        val br = perf.toRating
        updateRatings(wr, br, Black.some)
        wr.getRating must beCloseTo(1482d, 1d)
        br.getRating must beCloseTo(1517d, 1d)
        wr.getRatingDeviation must beCloseTo(78d, 1d)
        br.getRatingDeviation must beCloseTo(78d, 1d)
        wr.getVolatility must beCloseTo(0.06, 0.00001d)
        br.getVolatility must beCloseTo(0.06, 0.00001d)
      }
      "draw" in {
        val wr = perf.toRating
        val br = perf.toRating
        updateRatings(wr, br, None)
        wr.getRating must beCloseTo(1500d, 1d)
        br.getRating must beCloseTo(1500d, 1d)
        wr.getRatingDeviation must beCloseTo(78d, 1d)
        br.getRatingDeviation must beCloseTo(78d, 1d)
        wr.getVolatility must beCloseTo(0.06, 0.00001d)
        br.getVolatility must beCloseTo(0.06, 0.00001d)
      }
    }
    "mixed ratings and deviations" in {
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
      "white wins" in {
        val wr = wP.toRating
        val br = bP.toRating
        updateRatings(wr, br, White.some)
        wr.getRating must beCloseTo(1422d, 1d)
        br.getRating must beCloseTo(1506d, 1d)
        wr.getRatingDeviation must beCloseTo(77d, 1d)
        br.getRatingDeviation must beCloseTo(105d, 1d)
        wr.getVolatility must beCloseTo(0.06, 0.00001d)
        br.getVolatility must beCloseTo(0.065, 0.00001d)
      }
      "black wins" in {
        val wr = wP.toRating
        val br = bP.toRating
        updateRatings(wr, br, Black.some)
        wr.getRating must beCloseTo(1389d, 1d)
        br.getRating must beCloseTo(1568d, 1d)
        wr.getRatingDeviation must beCloseTo(78d, 1d)
        br.getRatingDeviation must beCloseTo(105d, 1d)
        wr.getVolatility must beCloseTo(0.06, 0.00001d)
        br.getVolatility must beCloseTo(0.065, 0.00001d)
      }
      "draw" in {
        val wr = wP.toRating
        val br = bP.toRating
        updateRatings(wr, br, None)
        wr.getRating must beCloseTo(1406d, 1d)
        br.getRating must beCloseTo(1537d, 1d)
        wr.getRatingDeviation must beCloseTo(78d, 1d)
        br.getRatingDeviation must beCloseTo(105.87d, 0.01d)
        wr.getVolatility must beCloseTo(0.06, 0.00001d)
        br.getVolatility must beCloseTo(0.065, 0.00001d)
      }
    }
  }
}
