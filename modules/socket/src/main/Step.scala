package lidraughts.socket

import draughts.format.Uci
import draughts.Pos

import play.api.libs.json._

case class Step(
    ply: Int,
    move: Option[Step.Move],
    fen: String,
    // None when not computed yet
    dests: Option[Map[Pos, List[Pos]]],
    captLen: Option[Int]
) {

  // who's color plays next
  def color = draughts.Color(ply % 2 == 0)

  def toJson = Step.stepJsonWriter writes this
}

object Step {

  case class Move(uci: Uci, san: String) {
    def uciString = uci.uci
    def shortUciString = uci.shortUci
  }

  private implicit val uciJsonWriter: Writes[Uci.Move] = Writes { uci =>
    JsString(uci.uci)
  }

  implicit val stepJsonWriter: Writes[Step] = Writes { step =>
    import step._
    Json.obj(
      "ply" -> ply,
      "uci" -> move.map(_.shortUciString),
      "san" -> move.map(_.san),
      "fen" -> fen,
      "captLen" -> ~captLen
    ).add("dests", dests.map {
        _.map {
          case (orig, dests) => s"${orig.piotr}${dests.map(_.piotr).mkString}"
        }.mkString(" ")
      })
  }
}
