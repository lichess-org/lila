package lila.rating

import reactivemongo.api.bson.{ BSONDocument, BSONDocumentHandler }

import lila.core.perf.Perf
import lila.core.rating.Glicko
import lila.db.BSON
import lila.rating.PerfExt.*

object GlickoExt:

  extension (g: Glicko)

    def intervalMin = (g.rating - g.deviation * 2).toInt
    def intervalMax = (g.rating + g.deviation * 2).toInt
    def interval    = g.intervalMin -> g.intervalMax

    def rankable(variant: chess.variant.Variant) =
      g.deviation <= {
        if variant.standard then Glicko.standardRankableDeviation
        else Glicko.variantRankableDeviation
      }

    def sanityCheck: Boolean =
      g.rating > 0 &&
        g.rating < 4000 &&
        g.deviation > 0 &&
        g.deviation < 1000 &&
        g.volatility > 0 &&
        g.volatility < (Glicko.maxVolatility * 2)

    def cap: Glicko =
      g.copy(
        rating = g.rating.atLeast(Glicko.minRating.value),
        deviation = g.deviation.atLeast(Glicko.minDeviation).atMost(Glicko.maxDeviation),
        volatility = g.volatility.atMost(Glicko.maxVolatility)
      )

    def average(other: Glicko, weight: Float = 0.5f): Glicko =
      if weight >= 1 then other
      else if weight <= 0 then g
      else
        new Glicko(
          rating = g.rating * (1 - weight) + other.rating * weight,
          deviation = g.deviation * (1 - weight) + other.deviation * weight,
          volatility = g.volatility * (1 - weight) + other.volatility * weight
        )

object Glicko:
  export lila.core.rating.Glicko.*

  val minRating = IntRating(400)
  val maxRating = IntRating(4000)

  val minDeviation              = 45
  val variantRankableDeviation  = 65
  val standardRankableDeviation = 75
  val maxDeviation              = 500d

  // past this, it might not stabilize ever again
  val maxVolatility     = 0.1d
  val defaultVolatility = 0.09d

  // Chosen so a typical player's RD goes from 60 -> 110 in 1 year
  val ratingPeriodsPerDay = 0.21436d

  val default = new Glicko(1500d, maxDeviation, defaultVolatility)

  // managed is for students invited to a class
  val defaultManaged       = new Glicko(800d, 400d, defaultVolatility)
  val defaultManagedPuzzle = new Glicko(800d, 400d, defaultVolatility)

  // bot accounts (usually a stockfish instance)
  val defaultBot = new Glicko(2000d, maxDeviation, defaultVolatility)

  // rating that can be lost or gained with a single game
  val maxRatingDelta = 700

  val tau    = 0.75d
  val system = glicko2.RatingCalculator(tau, ratingPeriodsPerDay)

  def liveDeviation(p: Perf, reverse: Boolean): Double = {
    system.previewDeviation(p.toRating, nowInstant, reverse)
  }.atLeast(minDeviation).atMost(maxDeviation)

  given glickoHandler: BSONDocumentHandler[Glicko] = new BSON[Glicko]:
    def reads(r: BSON.Reader): Glicko = new Glicko(
      rating = r.double("r"),
      deviation = r.double("d"),
      volatility = r.double("v")
    )
    def writes(w: BSON.Writer, o: Glicko) = BSONDocument(
      "r" -> w.double(o.rating),
      "d" -> w.double(o.deviation),
      "v" -> w.double(o.volatility)
    )

  import play.api.libs.json.{ OWrites, Json }
  given glickoWrites: OWrites[Glicko] =
    import scalalib.Maths.roundDownAt
    OWrites: p =>
      Json
        .obj(
          "rating"    -> roundDownAt(p.rating, 2),
          "deviation" -> roundDownAt(p.deviation, 2)
        )
        .add("provisional" -> p.provisional)

  enum Result:
    case Win, Loss, Draw
