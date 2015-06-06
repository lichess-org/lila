package lila.rating

import org.goochjs.glicko2.Rating
import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument

import lila.db.BSON

case class Perf(
    glicko: Glicko,
    nb: Int,
    recent: List[Int],
    latest: Option[DateTime]) {

  def intRating = glicko.rating.toInt
  def intDeviation = glicko.deviation.toInt

  def progress: Int = ~recent.headOption.flatMap { head =>
    recent.lastOption map (head-)
  }

  def add(g: Glicko, date: DateTime): Perf = copy(
    glicko = g,
    nb = nb + 1,
    recent =
      if (nb < 10) recent
      else (g.intRating :: recent) take Perf.recentMaxSize,
    latest = date.some)

  def add(r: Rating, date: DateTime): Perf = add(Glicko(r.getRating, r.getRatingDeviation, r.getVolatility), date)

  def toRating = new Rating(
    math.max(Glicko.minRating, glicko.rating),
    glicko.deviation,
    glicko.volatility,
    nb)

  def nonEmpty = nb > 0

  def provisional = glicko.provisional
}

case object Perf {

  type Key = String

  case class Typed(perf: Perf, perfType: PerfType)

  val default = Perf(Glicko.default, 0, Nil, None)

  val recentMaxSize = 12

  implicit val perfBSONHandler = new BSON[Perf] {

    import Glicko.glickoBSONHandler

    def reads(r: BSON.Reader): Perf = Perf(
      glicko = r.getO[Glicko]("gl") | Glicko.default,
      nb = r intD "nb",
      latest = r dateO "la",
      recent = r intsD "re")

    def writes(w: BSON.Writer, o: Perf) = BSONDocument(
      "gl" -> o.glicko,
      "nb" -> w.int(o.nb),
      "re" -> w.intsO(o.recent),
      "la" -> o.latest.map(w.date))
  }
}
