package lila.rating

import Math.{ abs, pow, sqrt }
import reactivemongo.bson.BSONDocument
import org.joda.time.{ DateTime, Days }

import lila.db.BSON

case class Glicko(
    rating: Double,
    deviation: Double,
    volatility: Double
) {

  def intRating = rating.toInt
  def intDeviation = deviation.toInt
  def intDeviationDoubled = (deviation * 2).toInt

  def intervalMin = (rating - deviation * 2).toInt
  def intervalMax = (rating + deviation * 2).toInt
  def interval = intervalMin -> intervalMax

  def provisional = deviation >= Glicko.provisionalDeviation
  def established = !provisional

  def establishedIntRating = established option intRating

  def refund(points: Int) = copy(rating = rating + points)

  def sanityCheck =
    rating > 0 &&
      rating < 4000 &&
      deviation > 0 &&
      deviation < 1000 &&
      volatility > 0 &&
      volatility < (Glicko.maxVolatility * 2)

  // for rating refunds, aging should somehow increase the rating deviation (hence abs)
  def age(latest: Option[DateTime], date: DateTime) = copy(deviation = sqrt(pow(deviation, 2) + pow(Glicko.c, 2) * abs(Glicko.periods(latest, date))))

  def cap = copy(deviation = deviation min Glicko.maxDeviation, volatility = volatility min Glicko.maxVolatility)

  override def toString = s"$intRating $intDeviation"
}

case object Glicko {

  val minRating = 800

  val maxDeviation = 350d
  val provisionalDeviation = 110d

  // amount to age the RD over a rating period
  // 60 months (rating periods) would need to pass before a rating for a typical
  // player becomes as uncertain as that of an unrated player
  val c = sqrt(pow(maxDeviation, 2) - pow(provisionalDeviation / 2, 2) / 60d)
  val daysPerPeriod = 30d
  def periods(latest: Option[DateTime], date: DateTime): Double = Days.daysBetween(date, latest | date).getDays / daysPerPeriod

  // past this, it might not stabilize ever again
  val maxVolatility = 0.1d

  val default = Glicko(1500d, maxDeviation, 0.06d)
  val defaultIntRating = default.rating.toInt

  def range(rating: Double, deviation: Double) = (
    rating - (deviation * 2),
    rating + (deviation * 2)
  )

  implicit val glickoBSONHandler = new BSON[Glicko] {

    def reads(r: BSON.Reader): Glicko = Glicko(
      rating = r double "r",
      deviation = r double "d",
      volatility = r double "v"
    )

    def writes(w: BSON.Writer, o: Glicko) = BSONDocument(
      "r" -> w.double(o.rating),
      "d" -> w.double(o.deviation),
      "v" -> w.double(o.volatility)
    )
  }

  sealed abstract class Result(val v: Double) {
    def negate: Result
  }
  object Result {
    case object Win extends Result(1) { def negate = Loss }
    case object Loss extends Result(0) { def negate = Win }
    case object Draw extends Result(0.5) { def negate = Draw }
  }
}
