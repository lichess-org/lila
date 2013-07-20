package lila.user

import chess.Speed
import lila.db.Tube
import play.api.libs.json._
import Tube.Helpers._

private[user] case class SpeedElo(nb: Int, elo: Int)

private[user] case object SpeedElo {

  val default = SpeedElo(0, User.STARTING_ELO)

  private[user] lazy val tube = Tube[SpeedElo](Json.reads[SpeedElo], Json.writes[SpeedElo])
}

private[user] case class SpeedElos(
    bullet: SpeedElo,
    blitz: SpeedElo,
    slow: SpeedElo) {

  def toMap = Map(
    Speed.Bullet -> bullet,
    Speed.Blitz -> blitz,
    Speed.Slow -> slow)
}

private[user] object SpeedElos {

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
