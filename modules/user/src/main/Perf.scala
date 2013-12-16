package lila.user

import reactivemongo.bson.BSONDocument

import lila.db.BSON

case class Perf(
    glicko: Glicko,
    nb: Int) {

  def intRating = glicko.rating.toInt
  def intDeviation = glicko.deviation.toInt

  override def toString = s"#$nb $intRating $intDeviation"
}

case object Perf {

  val default = Perf(Glicko.default, 0)

  private def PerfBSONHandler = new BSON[Perf] {

    implicit def glickoHandler = Glicko.tube.handler

    def reads(r: BSON.Reader): Perf = Perf(
      glicko = r.getO[Glicko]("gl") | Glicko.default,
      nb = r intD "nb")

    def writes(w: BSON.Writer, o: Perf) = BSONDocument(
      "gl" -> o.glicko,
      "nb" -> w.int(o.nb))
  }

  lazy val tube = lila.db.BsTube(PerfBSONHandler)
}

