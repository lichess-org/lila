package lila.game

import chess.Pos

import play.api.libs.json._

case class Step(
    move: Option[Step.Move],
    fen: String,
    check: Boolean,
    dests: Map[Pos, List[Pos]],
    eval: Option[Int] = None,
    mate: Option[Int] = None,
    comments: List[String] = Nil,
    variations: List[List[Step]] = Nil) {
}

object Step {

  case class Move(pos: (Pos, Pos), san: String) {
    def uci = s"${pos._1.key}${pos._2.key}"
  }

  implicit val stepJsonWriter: Writes[Step] = Writes { step =>
    import step._
    (
      add("check", true, check) _ compose
      add("eval", eval) _ compose
      add("mate", mate) _ compose
      add("comments", comments, comments.nonEmpty) _ compose
      add("variations", variations, variations.nonEmpty) _
    )(Json.obj(
        "uci" -> move.map(_.uci),
        "san" -> move.map(_.san),
        "fen" -> fen,
        "dests" -> dests.map {
          case (orig, dests) => s"${orig.piotr}${dests.map(_.piotr).mkString}"
        }.mkString(" ")))
  }

  private def add[A](k: String, v: A, cond: Boolean)(o: JsObject)(implicit writes: Writes[A]): JsObject =
    if (cond) o + (k -> writes.writes(v)) else o

  private def add[A: Writes](k: String, v: Option[A]): JsObject => JsObject =
    v.fold(identity[JsObject] _) { add(k, _, true) _ }
}
