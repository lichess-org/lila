package lila.rating

import reactivemongo.api.bson.{ BSONDocument, BSONDocumentHandler }

import lila.db.BSON

case class Glicko(
    rating: Double,
    deviation: Double,
    volatility: Double
):

  def intRating    = IntRating(rating.toInt)
  def intDeviation = deviation.toInt

  def intervalMin = (rating - deviation * 2).toInt
  def intervalMax = (rating + deviation * 2).toInt
  def interval    = intervalMin -> intervalMax

  def rankable(variant: chess.variant.Variant) =
    deviation <= {
      if (variant.standard) Glicko.standardRankableDeviation
      else Glicko.variantRankableDeviation
    }
  def provisional          = RatingProvisional(deviation >= Glicko.provisionalDeviation)
  def established          = provisional.no
  def establishedIntRating = established option intRating

  def clueless = deviation >= Glicko.cluelessDeviation

  def refund(points: Int) = copy(rating = rating + points)

  def sanityCheck =
    rating > 0 &&
      rating < 4000 &&
      deviation > 0 &&
      deviation < 1000 &&
      volatility > 0 &&
      volatility < (Glicko.maxVolatility * 2)

  def cap =
    copy(
      rating = rating atLeast Glicko.minRating.value,
      deviation = deviation atLeast Glicko.minDeviation atMost Glicko.maxDeviation,
      volatility = volatility atMost Glicko.maxVolatility
    )

  def average(other: Glicko, weight: Float = 0.5f) =
    if (weight >= 1) other
    else if (weight <= 0) this
    else
      Glicko(
        rating = rating * (1 - weight) + other.rating * weight,
        deviation = deviation * (1 - weight) + other.deviation * weight,
        volatility = volatility * (1 - weight) + other.volatility * weight
      )

  def display = s"$intRating${provisional.yes so "?"}"

  override def toString = f"$intRating/$intDeviation/${volatility}%.3f"

case object Glicko:

  val minRating = IntRating(400)
  val maxRating = IntRating(4000)

  val minDeviation              = 45
  val variantRankableDeviation  = 65
  val standardRankableDeviation = 75
  val provisionalDeviation      = 110
  val cluelessDeviation         = 230
  val maxDeviation              = 500d

  // past this, it might not stabilize ever again
  val maxVolatility     = 0.1d
  val defaultVolatility = 0.09d

  // Chosen so a typical player's RD goes from 60 -> 110 in 1 year
  val ratingPeriodsPerDay = 0.21436d

  val default = Glicko(1500d, maxDeviation, defaultVolatility)

  // managed is for students invited to a class
  val defaultManaged       = Glicko(800d, 400d, defaultVolatility)
  val defaultManagedPuzzle = Glicko(800d, 400d, defaultVolatility)

  // bot accounts (usually a stockfish instance)
  val defaultBot = Glicko(2000d, maxDeviation, defaultVolatility)

  // rating that can be lost or gained with a single game
  val maxRatingDelta = 700

  val tau    = 0.75d
  val system = glicko2.RatingCalculator(tau, ratingPeriodsPerDay)

  def liveDeviation(p: Perf, reverse: Boolean): Double = {
    system.previewDeviation(p.toRating, nowInstant, reverse)
  } atLeast minDeviation atMost maxDeviation

  given BSONDocumentHandler[Glicko] = new BSON[Glicko]:

    def reads(r: BSON.Reader): Glicko =
      Glicko(
        rating = r double "r",
        deviation = r double "d",
        volatility = r double "v"
      )

    def writes(w: BSON.Writer, o: Glicko) =
      BSONDocument(
        "r" -> w.double(o.rating),
        "d" -> w.double(o.deviation),
        "v" -> w.double(o.volatility)
      )

  import play.api.libs.json.{ OWrites, Json }
  given OWrites[Glicko] =
    import lila.common.Maths.roundDownAt
    OWrites: p =>
      Json
        .obj(
          "rating"    -> roundDownAt(p.rating, 2),
          "deviation" -> roundDownAt(p.deviation, 2)
        )
        .add("provisional" -> p.provisional)

  enum Result:
    case Win, Loss, Draw
