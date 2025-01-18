package lila.socket

import play.api.libs.json._

import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi

case class Step(
    ply: Int,
    usi: Option[Usi],
    sfen: Sfen,
    check: Boolean
) {

  // who's color plays next
  def color = shogi.Color.fromPly(ply)

  def toJson = Step.stepJsonWriter writes this
}

object Step {

  implicit val stepJsonWriter: Writes[Step] = Writes { step =>
    import lila.common.Json._
    import step._
    Json
      .obj(
        "ply"  -> ply,
        "usi"  -> usi.map(_.usi),
        "sfen" -> sfen
      )
      .add("check", check)
  }
}
