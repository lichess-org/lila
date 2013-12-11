package lila.user

import lila.db.BSON
import reactivemongo.bson.BSONDocument

case class SubElo(nb: Int, elo: Int) {

  def countRated = nb // for compat with chess elo calculator

  def addGame(newElo: Int) = SubElo(nb = nb + 1, elo = newElo)

  def withElo(e: Int) = copy(elo = e)
}

case object SubElo {

  val default = SubElo(0, User.STARTING_ELO)

  private def subEloBSONHandler = new BSON[SubElo] {

    def reads(r: BSON.Reader): SubElo = SubElo(
      nb = r nInt "nb",
      elo = r nInt "elo")

    def writes(w: BSON.Writer, o: SubElo) = BSONDocument(
      "nb" -> w.int(o.nb),
      "elo" -> w.int(o.elo))
  }

  private[user] lazy val tube = lila.db.BsTube(subEloBSONHandler)
}
