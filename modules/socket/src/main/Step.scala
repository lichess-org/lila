package lila.socket

import shogi.format.Uci
import shogi.{ Hand, Hands, Pos }

import play.api.libs.json._

case class Step(
    ply: Int,
    move: Option[Step.Move],
    fen: String,
    check: Boolean,
    // None when not computed yet
    dests: Option[Map[Pos, List[Pos]]],
    drops: Option[List[Pos]]
) {

  // who's color plays next
  def color = shogi.Color.fromPly(ply)

  def toJson = Step.stepJsonWriter writes this
}

object Step {

  case class Move(uci: Uci, san: String) {
    def uciString = uci.uci
    def usiString = uci.usi
  }

  implicit val stepJsonWriter: Writes[Step] = Writes { step =>
    import step._
    Json
      .obj(
        "ply" -> ply,
        "uci" -> move.map(_.uciString),
        //"usi" -> move.map(_.usiString),
        "san" -> move.map(_.san),
        "fen" -> fen
      )
      .add("check", check)
      .add(
        "dests",
        dests.map {
          _.map { case (orig, dests) =>
            s"${orig.piotr}${dests.map(_.piotr).mkString}"
          }.mkString(" ")
        }
      )
      .add(
        "drops",
        drops.map { drops =>
          JsString(drops.map(_.uciKey).mkString)
        }
      )
  }
}
