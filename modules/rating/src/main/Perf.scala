package lila.rating

import org.goochjs.glicko2.Rating
import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument

import lila.db.BSON

case class Perf(
    glicko: Glicko,
    nb: Int,
    recent: List[Int],
    latest: Option[DateTime]
) {

  def intRating = glicko.rating.toInt
  def intDeviation = glicko.deviation.toInt

  def progress: Int = ~recent.headOption.flatMap { head =>
    recent.lastOption map (head-)
  }

  def add(g: Glicko, date: DateTime): Perf = copy(
    glicko = g.cap,
    nb = nb + 1,
    recent = updateRecentWith(g),
    latest = date.some
  )

  def add(r: Rating, date: DateTime): Option[Perf] = {
    val glicko = Glicko(r.getRating, r.getRatingDeviation, r.getVolatility)
    glicko.sanityCheck option add(glicko, date)
  }

  def addOrReset(monitor: lila.mon.IncPath, msg: => String)(r: Rating, date: DateTime): Perf = add(r, date) | {
    lila.log("rating").error(s"Crazy Glicko2 $msg")
    lila.mon.incPath(monitor)()
    add(Glicko.default, date)
  }

  def averageGlicko(other: Perf) = copy(
    glicko = glicko average other.glicko
  )

  def refund(points: Int): Perf = {
    val newGlicko = glicko refund points
    copy(
      glicko = newGlicko,
      recent = updateRecentWith(newGlicko)
    )
  }

  private def updateRecentWith(glicko: Glicko) =
    if (nb < 10) recent
    else (glicko.intRating :: recent) take Perf.recentMaxSize

  def toRating = new Rating(
    math.max(Glicko.minRating, glicko.rating),
    glicko.deviation,
    glicko.volatility,
    nb
  )

  def isEmpty = nb == 0
  def nonEmpty = !isEmpty

  def provisional = glicko.provisional
  def established = glicko.established
}

case object Perf {

  type Key = String
  type ID = Int

  case class Typed(perf: Perf, perfType: PerfType)

  val default = Perf(Glicko.default, 0, Nil, None)

  val recentMaxSize = 12

  implicit val perfBSONHandler = new BSON[Perf] {

    import Glicko.glickoBSONHandler

    def reads(r: BSON.Reader): Perf = Perf(
      glicko = r.getO[Glicko]("gl") | Glicko.default,
      nb = r intD "nb",
      latest = r dateO "la",
      recent = r intsD "re"
    )

    def writes(w: BSON.Writer, o: Perf) = BSONDocument(
      "gl" -> o.glicko,
      "nb" -> w.int(o.nb),
      "re" -> w.listO(o.recent),
      "la" -> o.latest.map(w.date)
    )
  }
}
