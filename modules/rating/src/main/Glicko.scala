package lila.rating

import reactivemongo.api.bson.{ BSONDocument, BSONDocumentHandler }
import chess.IntRating
import chess.rating.glicko.{ Glicko, GlickoCalculator, RatingPeriodsPerDay }

import lila.core.perf.Perf
import lila.db.BSON
import lila.rating.PerfExt.*

object GlickoExt:

  extension (g: Glicko)

    def intervalMin = (g.rating - g.deviation * 2).toInt
    def intervalMax = (g.rating + g.deviation * 2).toInt
    def interval = g.intervalMin -> g.intervalMax

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

object Glicko:
  export chess.rating.glicko.Glicko.*

  val minRating: IntRating = IntRating(400)
  val maxRating: IntRating = IntRating(4000)

  val minDeviation = 45
  val variantRankableDeviation = 65
  val standardRankableDeviation = 75
  val maxDeviation = 500d

  // past this, it might not stabilize ever again
  val maxVolatility = 0.1d
  val defaultVolatility = 0.09d

  val default = new Glicko(1500d, maxDeviation, defaultVolatility)

  // Virtual rating for first pairing to make the expected score 50% without
  // actually changing the default rating
  val pairingDefault = new Glicko(1450d, maxDeviation, defaultVolatility)

  // managed is for students invited to a class
  val defaultManaged = new Glicko(800d, 400d, defaultVolatility)
  val defaultManagedPuzzle = new Glicko(800d, 400d, defaultVolatility)

  // bot accounts (usually a stockfish instance)
  val defaultBot = new Glicko(2000d, maxDeviation, defaultVolatility)

  // rating that can be lost or gained with a single game
  val maxRatingDelta = 700

  val calculator = GlickoCalculator(
    // Chosen so a typical player's RD goes from 60 -> 110 in 1 year
    ratingPeriodsPerDay = RatingPeriodsPerDay(0.21436d)
  )

  def liveDeviation(p: Perf, reverse: Boolean): Double = {
    calculator.previewDeviation(p.toGlickoPlayer, nowInstant, reverse)
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
          "rating" -> roundDownAt(p.rating, 2),
          "deviation" -> roundDownAt(p.deviation, 2)
        )
        .add("provisional" -> p.provisional)

  enum Result:
    case Win, Loss, Draw
