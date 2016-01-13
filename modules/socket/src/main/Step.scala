package lila.socket

import chess.format.UciMove
import chess.Pos

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._

case class Step(
    ply: Int,
    move: Option[Step.Move],
    fen: String,
    check: Boolean,
    // None when not computed yet
    dests: Option[Map[Pos, List[Pos]]],
    eval: Option[Step.Eval] = None,
    nag: Option[String] = None,
    comments: List[String] = Nil,
    variations: List[List[Step]] = Nil) {

  // who's color plays next
  def color = chess.Color(ply % 2 == 0)

  def toJson = Step.stepJsonWriter writes this
}

object Step {

  case class Move(uci: UciMove, san: String) {
    def uciString = uci.uci
  }

  case class Eval(
    cp: Option[Int] = None,
    mate: Option[Int] = None,
    best: Option[UciMove])

  private implicit val uciJsonWriter: Writes[UciMove] = Writes { uci =>
    JsString(uci.uci)
  }
  private implicit val evalJsonWriter = Json.writes[Eval]

  implicit val stepJsonWriter: Writes[Step] = Writes { step =>
    import step._
    (
      add("check", true, check) _ compose
      add("eval", eval) _ compose
      add("nag", nag) _ compose
      add("comments", comments, comments.nonEmpty) _ compose
      add("variations", variations, variations.nonEmpty) _ compose
      add("dests", dests.map {
        _.map {
          case (orig, dests) => s"${orig.piotr}${dests.map(_.piotr).mkString}"
        }.mkString(" ")
      })
    )(Json.obj(
        "ply" -> ply,
        "uci" -> move.map(_.uciString),
        "san" -> move.map(_.san),
        "fen" -> fen))
  }

  private def add[A](k: String, v: A, cond: Boolean)(o: JsObject)(implicit writes: Writes[A]): JsObject =
    if (cond) o + (k -> writes.writes(v)) else o

  private def add[A: Writes](k: String, v: Option[A]): JsObject => JsObject =
    v.fold(identity[JsObject] _) { add(k, _, true) _ }
}
