package lila.user

import reactivemongo.bson.BSONDocument

import lila.db.BSON

case class Glicko(
  rating: Double,
  rd: Double,
  volatility: Double)

case object Glicko {

  val default = Glicko(1500d, 350d, 0.6d)

  private def GlickoBSONHandler = new BSON[Glicko] {

    def reads(r: BSON.Reader): Glicko = Glicko(
      rating = r double "r",
      rd = r double "rd",
      volatility = r double "v")

    def writes(w: BSON.Writer, o: Glicko) = BSONDocument(
      "r" -> w.double(o.rating),
      "rd" -> w.double(o.rd),
      "v" -> w.double(o.volatility))
  }

  private[user] lazy val tube = lila.db.BsTube(GlickoBSONHandler)
}
