package lila.rating

import org.specs2.mutable.Specification
import glicko2.*
import chess.{ Color, White, Black }

class RatingCalculatorTest extends Specification {

  sequential

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

  "compute rating" >> {
    "default deviation" >> {
      "white wins" >> {
        val wr = Perf.default.toRating
        val br = Perf.default.toRating
        updateRatings(wr, br, White.some)
        wr.rating must beCloseTo(1741d, 1d)
        br.rating must beCloseTo(1258d, 1d)
        wr.ratingDeviation must beCloseTo(396d, 1d)
        br.ratingDeviation must beCloseTo(396d, 1d)
        wr.volatility must beCloseTo(0.0899983, 0.00000001d)
        br.volatility must beCloseTo(0.0899983, 0.0000001d)
      }
      "black wins" >> {
        val wr = Perf.default.toRating
        val br = Perf.default.toRating
        updateRatings(wr, br, Black.some)
        wr.rating must beCloseTo(1258d, 1d)
        br.rating must beCloseTo(1741d, 1d)
        wr.ratingDeviation must beCloseTo(396d, 1d)
        br.ratingDeviation must beCloseTo(396d, 1d)
        wr.volatility must beCloseTo(0.0899983, 0.00000001d)
        br.volatility must beCloseTo(0.0899983, 0.0000001d)
      }
      "draw" >> {
        val wr = Perf.default.toRating
        val br = Perf.default.toRating
        updateRatings(wr, br, None)
        wr.rating must beCloseTo(1500d, 1d)
        br.rating must beCloseTo(1500d, 1d)
        wr.ratingDeviation must beCloseTo(396d, 1d)
        br.ratingDeviation must beCloseTo(396d, 1d)
        wr.volatility must beCloseTo(0.0899954, 0.0000001d)
        br.volatility must beCloseTo(0.0899954, 0.0000001d)
      }
    }
    "low deviation" >> {
      val perf = Perf.default.copy(glicko =
        Glicko.default.copy(
          deviation = 80,
          volatility = 0.06
        )
      )
      "white wins" >> {
        val wr = perf.toRating
        val br = perf.toRating
        updateRatings(wr, br, White.some)
        wr.rating must beCloseTo(1517d, 1d)
        br.rating must beCloseTo(1482d, 1d)
        wr.ratingDeviation must beCloseTo(78d, 1d)
        br.ratingDeviation must beCloseTo(78d, 1d)
        wr.volatility must beCloseTo(0.06, 0.00001d)
        br.volatility must beCloseTo(0.06, 0.00001d)
      }
      "black wins" >> {
        val wr = perf.toRating
        val br = perf.toRating
        updateRatings(wr, br, Black.some)
        wr.rating must beCloseTo(1482d, 1d)
        br.rating must beCloseTo(1517d, 1d)
        wr.ratingDeviation must beCloseTo(78d, 1d)
        br.ratingDeviation must beCloseTo(78d, 1d)
        wr.volatility must beCloseTo(0.06, 0.00001d)
        br.volatility must beCloseTo(0.06, 0.00001d)
      }
      "draw" >> {
        val wr = perf.toRating
        val br = perf.toRating
        updateRatings(wr, br, None)
        wr.rating must beCloseTo(1500d, 1d)
        br.rating must beCloseTo(1500d, 1d)
        wr.ratingDeviation must beCloseTo(78d, 1d)
        br.ratingDeviation must beCloseTo(78d, 1d)
        wr.volatility must beCloseTo(0.06, 0.00001d)
        br.volatility must beCloseTo(0.06, 0.00001d)
      }
    }
    "mixed ratings and deviations" >> {
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
      "white wins" >> {
        val wr = wP.toRating
        val br = bP.toRating
        updateRatings(wr, br, White.some)
        wr.rating must beCloseTo(1422d, 1d)
        br.rating must beCloseTo(1506d, 1d)
        wr.ratingDeviation must beCloseTo(77d, 1d)
        br.ratingDeviation must beCloseTo(105d, 1d)
        wr.volatility must beCloseTo(0.06, 0.00001d)
        br.volatility must beCloseTo(0.065, 0.00001d)
      }
      "black wins" >> {
        val wr = wP.toRating
        val br = bP.toRating
        updateRatings(wr, br, Black.some)
        wr.rating must beCloseTo(1389d, 1d)
        br.rating must beCloseTo(1568d, 1d)
        wr.ratingDeviation must beCloseTo(78d, 1d)
        br.ratingDeviation must beCloseTo(105d, 1d)
        wr.volatility must beCloseTo(0.06, 0.00001d)
        br.volatility must beCloseTo(0.065, 0.00001d)
      }
      "draw" >> {
        val wr = wP.toRating
        val br = bP.toRating
        updateRatings(wr, br, None)
        wr.rating must beCloseTo(1406d, 1d)
        br.rating must beCloseTo(1537d, 1d)
        wr.ratingDeviation must beCloseTo(78d, 1d)
        br.ratingDeviation must beCloseTo(105.87d, 0.01d)
        wr.volatility must beCloseTo(0.06, 0.00001d)
        br.volatility must beCloseTo(0.065, 0.00001d)
      }
    }
    "more mixed ratings and deviations" >> {
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
      "white wins" >> {
        val wr = wP.toRating
        val br = bP.toRating
        updateRatings(wr, br, White.some)
        wr.rating must beCloseTo(1216.7d, 0.1d)
        br.rating must beCloseTo(1636d, 0.1d)
        wr.ratingDeviation must beCloseTo(59.9d, 0.1d)
        br.ratingDeviation must beCloseTo(196.9d, 0.1d)
        wr.volatility must beCloseTo(0.053013, 0.000001d)
        br.volatility must beCloseTo(0.062028, 0.000001d)
      }
      "black wins" >> {
        val wr = wP.toRating
        val br = bP.toRating
        updateRatings(wr, br, Black.some)
        wr.rating must beCloseTo(1199.3d, 0.1d)
        br.rating must beCloseTo(1855.4d, 0.1d)
        wr.ratingDeviation must beCloseTo(59.9d, 0.1d)
        br.ratingDeviation must beCloseTo(196.9d, 0.1d)
        wr.volatility must beCloseTo(0.052999, 0.000001d)
        br.volatility must beCloseTo(0.061999, 0.000001d)
      }
      "draw" >> {
        val wr = wP.toRating
        val br = bP.toRating
        updateRatings(wr, br, None)
        wr.rating must beCloseTo(1208.0, 0.1d)
        br.rating must beCloseTo(1745.7, 0.1d)
        wr.ratingDeviation must beCloseTo(59.90056, 0.1d)
        br.ratingDeviation must beCloseTo(196.98729, 0.1d)
        wr.volatility must beCloseTo(0.053002, 0.000001d)
        br.volatility must beCloseTo(0.062006, 0.000001d)
      }
    }
  }
}
