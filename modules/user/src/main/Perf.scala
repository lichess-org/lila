package lila.user

import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument

import lila.rating.Glicko
import lila.db.BSON

case class Perf(
    glicko: Glicko,
    nb: Int,
    latest: Option[DateTime]) {

  def intRating = glicko.rating.toInt
  def intDeviation = glicko.deviation.toInt

  override def toString = s"#$nb $intRating $intDeviation"
}

case object Perf {

  val default = Perf(Glicko.default, 0, None)

  val titles = Map(
    "bullet"   -> "Very fast games: less than 3 minutes",
    "blitz"    -> "Fast games: less than 8 minutes",
    "slow"     -> "Slow games: more than 8 minutes",
    "standard" -> "Standard rules of chess",
    "chess960" -> "Chess960 variant",
    "white"    -> "With white pieces",
    "black"    -> "With black pieces")

  private def PerfBSONHandler = new BSON[Perf] {

    implicit def glickoHandler = Glicko.tube.handler

    def reads(r: BSON.Reader): Perf = Perf(
      glicko = r.getO[Glicko]("gl") | Glicko.default,
      nb = r intD "nb",
      latest = r dateO "la")

    def writes(w: BSON.Writer, o: Perf) = BSONDocument(
      "gl" -> o.glicko,
      "nb" -> w.int(o.nb),
      "la" -> o.latest.map(w.date))
  }

  lazy val tube = lila.db.BsTube(PerfBSONHandler)
}

