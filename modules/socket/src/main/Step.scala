package lila.socket

import chess.format.Uci
import chess.opening.FullOpening
import chess.Pos
import chess.variant.Crazyhouse

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
    drops: Option[List[Pos]],
    eval: Option[Step.Eval] = None,
    nag: Option[String] = None,
    comments: List[String] = Nil,
    variations: List[List[Step]] = Nil,
    opening: Option[FullOpening] = None,
    crazyData: Option[Crazyhouse.Data]) {

  // who's color plays next
  def color = chess.Color(ply % 2 == 0)

  def toJson = Step.stepJsonWriter writes this
}

object Step {

  case class Move(uci: Uci, san: String) {
    def uciString = uci.uci
  }

  case class Eval(
    cp: Option[Int] = None,
    mate: Option[Int] = None,
    best: Option[Uci.Move])

  private implicit val uciJsonWriter: Writes[Uci.Move] = Writes { uci =>
    JsString(uci.uci)
  }
  private implicit val evalJsonWriter = Json.writes[Eval]

  // TODO copied from lila.game
  // put all that shit somewhere else
  private implicit val crazyhousePocketWriter: OWrites[Crazyhouse.Pocket] = OWrites { v =>
    JsObject(
      Crazyhouse.storableRoles.flatMap { role =>
        Some(v.roles.count(role ==)).filter(0 <).map { count =>
          role.name -> JsNumber(count)
        }
      })
  }
  private implicit val crazyhouseDataWriter: OWrites[chess.variant.Crazyhouse.Data] = OWrites { v =>
    Json.obj("pockets" -> List(v.pockets.white, v.pockets.black))
  }

  private[socket] implicit val openingWriter: OWrites[chess.opening.FullOpening] = OWrites { o =>
    Json.obj(
      "eco" -> o.eco,
      "name" -> o.name)
  }

  implicit val stepJsonWriter: Writes[Step] = Writes { step =>
    import step._
    (
      add("check", true, check) _ compose
      add("eval", eval) _ compose
      add("nag", nag) _ compose
      add("comments", comments, comments.nonEmpty) _ compose
      add("variations", variations, variations.nonEmpty) _ compose
      add("opening", opening) _ compose
      add("dests", dests.map {
        _.map {
          case (orig, dests) => s"${orig.piotr}${dests.map(_.piotr).mkString}"
        }.mkString(" ")
      }) _ compose
      add("drops", drops.map { drops =>
        JsString(drops.map(_.key).mkString)
      }) _ compose
      add("crazy", crazyData)
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
