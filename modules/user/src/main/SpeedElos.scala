package lila.user

import chess.Speed
import lila.db.JsTube
import play.api.libs.json._
import JsTube.Helpers._

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

  import reactivemongo.bson.Macros
  private implicit def subEloBsTube = SubElo.bsTube.handler
  private[user] lazy val bsTube = lila.db.BsTube(Macros.handler[SpeedElos])

  private implicit def subEloTube = SubElo.tube

  private[user] lazy val tube = JsTube[SpeedElos](
    __.json update merge(defaults) andThen Json.reads[SpeedElos],
    Json.writes[SpeedElos])

  private def defaults = Json.obj(
    "blitz" -> SubElo.default,
    "bullet" -> SubElo.default,
    "slow" -> SubElo.default
  )
}
