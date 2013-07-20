package lila.user

import chess.Speed
import lila.db.Tube
import play.api.libs.json._
import Tube.Helpers._

case class SpeedElo(nb: Int, elo: Int) {

  def countRated = nb // for compat with chess elo calculator

  def addGame(newElo: Int) = SpeedElo(nb = nb + 1, elo = newElo)

  def withElo(e: Int) = copy(elo = e)
}

case object SpeedElo {

  val default = SpeedElo(0, User.STARTING_ELO)

  private[user] lazy val tube = Tube[SpeedElo](Json.reads[SpeedElo], Json.writes[SpeedElo])
}

case class SpeedElos(
    bullet: SpeedElo,
    blitz: SpeedElo,
    slow: SpeedElo) {

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
        case SpeedElo(nb, elo) ⇒ nb * elo
      }).sum / nb
      val diff = to - median
      def amortize(se: SpeedElo) = se withElo (se.elo + (diff * se.nb / nb))
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
    SpeedElo.default,
    SpeedElo.default,
    SpeedElo.default)

  private implicit def speedEloTube = SpeedElo.tube

  private[user] lazy val tube = Tube[SpeedElos](
    __.json update merge(defaults) andThen Json.reads[SpeedElos],
    Json.writes[SpeedElos])

  private def defaults = Json.obj(
    "bullet" -> SpeedElo.default
  )
}
