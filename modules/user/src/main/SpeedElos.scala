package lila.user

import chess.Speed

case class SpeedElos(
    bullet: SubElo,
    blitz: SubElo,
    slow: SubElo) {

  def apply(speed: Speed) = speed match {
    case Speed.Bullet ⇒ bullet
    case Speed.Blitz  ⇒ blitz
    case _            ⇒ slow
  }

  def toMap = Map(
    Speed.Bullet -> bullet,
    Speed.Blitz -> blitz,
    Speed.Slow -> slow)

  def addGame(speed: Speed, newElo: Int) = speed match {
    case Speed.Bullet ⇒ copy(bullet = bullet addGame newElo)
    case Speed.Blitz  ⇒ copy(blitz = blitz addGame newElo)
    case _            ⇒ copy(slow = slow addGame newElo)
  }

  def adjustTo(to: Int) = {
    val nb = toMap.values.map(_.nb).sum
    if (nb == 0) this else {
      val median = (toMap.values map {
        case SubElo(nb, elo) ⇒ nb * elo
      }).sum / nb
      val diff = to - median
      def amortize(se: SubElo) = se withElo (se.elo + (diff * se.nb / nb))
      SpeedElos(
        bullet = amortize(bullet),
        blitz = amortize(blitz),
        slow = amortize(slow)
      )
    }
  }
}

object SpeedElos {

  val default = SpeedElos(
    SubElo.default,
    SubElo.default,
    SubElo.default)

  import lila.db.BSON
  import reactivemongo.bson.BSONDocument

  private def speedElosBSONHandler = new BSON[SpeedElos] {

    implicit def subEloHandler = SubElo.tube.handler

    def reads(r: BSON.Reader): SpeedElos = SpeedElos(
      blitz = r.getO[SubElo]("blitz") | default.blitz,
      bullet = r.getO[SubElo]("bullet") | default.bullet,
      slow = r.getO[SubElo]("slow") | default.slow)

    def writes(w: BSON.Writer, o: SpeedElos) = BSONDocument(
      "blitz" -> o.blitz,
      "bullet" -> o.bullet,
      "slow" -> o.slow)
  }

  private[user] lazy val tube = lila.db.BsTube(speedElosBSONHandler)
}
