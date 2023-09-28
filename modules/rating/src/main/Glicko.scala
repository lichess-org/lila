package lila.rating

import org.goochjs.glicko2._
import org.joda.time.DateTime
import reactivemongo.api.bson.BSONDocument

import lila.db.BSON

case class Glicko(
    rating: Double,
    deviation: Double,
    volatility: Double
) {

  def intRating           = rating.toInt
  def intDeviation        = deviation.toInt
  def intDeviationDoubled = (deviation * 2).toInt

  def intervalMin = (rating - deviation * 2).toInt
  def intervalMax = (rating + deviation * 2).toInt
  def interval    = intervalMin -> intervalMax

  def rankable(variant: shogi.variant.Variant) =
    deviation <= {
      if (variant.standard) Glicko.standardRankableDeviation
      else if (variant.chushogi) Glicko.chushogiRankableDeviation
      else if (variant.annanshogi) Glicko.annanshogiRankableDeviation
      else Glicko.variantRankableDeviation
    }
  def provisional          = deviation >= Glicko.provisionalDeviation
  def established          = !provisional
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
      rating = rating atLeast Glicko.minRating,
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

  def display = s"$intRating${provisional ?? "?"}"

  override def toString = s"$intRating $intDeviation"
}

case object Glicko {

  val minRating = 600

  val default = Glicko(1500d, 350d, 0.06d)

  val defaultManaged = Glicko(1000d, 350d, 0.06d)

  val defaultManagedPuzzle = Glicko(800d, 350d, 0.06d)

  val defaultIntRating = default.rating.toInt

  val minDeviation                = 45
  val chushogiRankableDeviation   = 85
  val annanshogiRankableDeviation = 80
  val variantRankableDeviation    = 70
  val standardRankableDeviation   = 75
  val provisionalDeviation        = 110
  val cluelessDeviation           = 260
  val maxDeviation                = 500d

  // past this, it might not stabilize ever again
  val maxVolatility = 0.1d

  // Chosen so a typical player's RD goes from 60 -> 110 in 1 year
  val ratingPeriodsPerDay = 0.21436d

  val maxRatingDelta = 700

  val tau    = 0.75d
  val system = new RatingCalculator(default.volatility, tau, ratingPeriodsPerDay)

  def range(rating: Double, deviation: Double) =
    (
      rating - (deviation * 2),
      rating + (deviation * 2)
    )

  def liveDeviation(p: Perf, reverse: Boolean): Double = {
    system.previewDeviation(p.toRating, new DateTime, reverse)
  } atLeast minDeviation atMost maxDeviation

  implicit val glickoBSONHandler = new BSON[Glicko] {

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
  }

  sealed abstract class Result {
    def negate: Result
  }
  object Result {
    case object Win  extends Result { def negate = Loss }
    case object Loss extends Result { def negate = Win  }
    case object Draw extends Result { def negate = Draw }
  }
}
