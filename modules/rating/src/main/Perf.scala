package lila.rating

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

  def progress: Option[Int] = recent.headOption flatMap { head =>
    recent.lastOption map { last =>
      head - last
    }
  }

  override def toString = s"#$nb $intRating $intDeviation"
}

case object Perf {

  val default = Perf(Glicko.default, 0, Nil, None)

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

  lazy val tube = lila.db.BsTube(perfBSONHandler)
}

