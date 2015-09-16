package lila.socket

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
    eval: Option[Int] = None,
    mate: Option[Int] = None,
    nag: Option[String] = None,
    comments: List[String] = Nil,
    variations: List[List[Step]] = Nil) {

  // who's color plays next
  def color = chess.Color(ply % 2 == 0)

  def toJson = Step.stepJsonWriter writes this
}

object Step {

  case class Move(orig: Pos, dest: Pos, san: String) {
    def uci = s"$orig$dest"
  }

  implicit val stepJsonWriter: Writes[Step] = Writes { step =>
    import step._
    (
      add("check", true, check) _ compose
      add("eval", eval) _ compose
      add("mate", mate) _ compose
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
        "uci" -> move.map(_.uci),
        "san" -> move.map(_.san),
        "fen" -> fen))
  }

  private implicit val stepMoveOptionJsonReader: Reads[Option[Move]] = (
    (__ \ "uci").readNullable[String] and
    (__ \ "san").readNullable[String]
  ) { (uci, san) =>
      for {
        orig <- uci.map(_ take 2) flatMap Pos.posAt
        dest <- uci.map(_ drop 2 take 2) flatMap Pos.posAt
        neSan <- san.filter(_.nonEmpty)
      } yield Move(orig, dest, neSan)
    }

  implicit val stepJsonReader: Reads[Step] = (
    (__ \ "ply").read[Int](min(0)) and
    (__ \ "move").read[Option[Move]] and
    (__ \ "fen").read[String] and
    (__ \ "check").read[Boolean] and
    (__ \ "dests").readNullable[String].map {
      _ map {
        _.split(' ').map(_.toList).flatMap {
          case first :: rest => for {
            orig <- Pos piotr first
            dests <- rest.flatMap(Pos.piotr).some.filter(_.nonEmpty)
          } yield orig -> dests
          case _ => None
        }.toMap
      }
    } and
    (__ \ "eval").readNullable[Int] and
    (__ \ "mate").readNullable[Int] and
    (__ \ "nag").readNullable[String] and
    (__ \ "comments").readNullable[List[String]].map(~_) and
    (__ \ "variations").readNullable[List[List[Step]]].map(~_)
  )(Step.apply _)

  private def add[A](k: String, v: A, cond: Boolean)(o: JsObject)(implicit writes: Writes[A]): JsObject =
    if (cond) o + (k -> writes.writes(v)) else o

  private def add[A: Writes](k: String, v: Option[A]): JsObject => JsObject =
    v.fold(identity[JsObject] _) { add(k, _, true) _ }
}
