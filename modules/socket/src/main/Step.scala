package lila.socket

import shogi.format.Uci
import shogi.{ Hand, Hands, Pos, Role }
import shogi.variant.Standard

import play.api.libs.json._

case class Step(
    ply: Int,
    move: Option[Step.Move],
    fen: String,
    check: Boolean,
    // None when not computed yet
    dests: Option[Map[Pos, List[Pos]]],
    drops: Option[List[Pos]],
    crazyData: Option[Hands]
) {

  // who's color plays next
  def color = shogi.Color(ply % 2 == 0)

  def toJson = Step.stepJsonWriter writes this
}

object Step {

  case class Move(uci: Uci, san: String) {
    def uciString = uci.uci
  }

  // TODO copied from lila.game
  // put all that shit somewhere else
  implicit val crazyhousePocketWriter: OWrites[Hand] = OWrites { h =>
    JsObject(
      h.roleMap.filter(kv => 0 < kv._2).map { kv =>
        kv._1.name -> JsNumber(kv._2)
      }
    )
  }

  implicit val crazyhouseDataWriter: OWrites[Hands] = OWrites { v =>
    Json.obj("pockets" -> List(v.sente, v.gote))
  }

  implicit val stepJsonWriter: Writes[Step] = Writes { step =>
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
