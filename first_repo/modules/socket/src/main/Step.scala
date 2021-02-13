package lila.socket

import chess.format.{ FEN, Uci }
import chess.Pos
import chess.variant.Crazyhouse
import play.api.libs.json._

case class Step(
    ply: Int,
    move: Option[Step.Move],
    fen: FEN,
    check: Boolean,
    // None when not computed yet
    dests: Option[Map[Pos, List[Pos]]],
    drops: Option[List[Pos]],
    crazyData: Option[Crazyhouse.Data]
) {

  // who's color plays next
  def color = chess.Color.fromPly(ply)

  def toJson = Step.stepJsonWriter writes this
}

object Step {

  case class Move(uci: Uci, san: String) {
    def uciString = uci.uci
  }

  // TODO copied from lila.game
  // put all that shit somewhere else
  implicit private val crazyhousePocketWriter: OWrites[Crazyhouse.Pocket] = OWrites { v =>
    JsObject(
      Crazyhouse.storableRoles.flatMap { role =>
        Some(v.roles.count(role ==)).filter(0 <).map { count =>
          role.name -> JsNumber(count)
        }
      }
    )
  }
  implicit private val crazyhouseDataWriter: OWrites[chess.variant.Crazyhouse.Data] = OWrites { v =>
    Json.obj("pockets" -> List(v.pockets.white, v.pockets.black))
  }

  implicit val stepJsonWriter: Writes[Step] = Writes { step =>
    import lila.common.Json._
    import step._
    Json
      .obj(
        "ply" -> ply,
        "uci" -> move.map(_.uciString),
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
          JsString(drops.map(_.key).mkString)
        }
      )
      .add("crazy", crazyData)
  }
}
