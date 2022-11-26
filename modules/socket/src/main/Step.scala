package lila.socket

import chess.format.{ FEN, Uci }
import chess.Pos
import chess.variant.Crazyhouse
import play.api.libs.json.*

case class Step(
    ply: Int,
    move: Option[Step.Move],
    fen: FEN,
    check: Boolean,
    // None when not computed yet
    dests: Option[Map[Pos, List[Pos]]],
    drops: Option[List[Pos]],
    crazyData: Option[Crazyhouse.Data]
):

  // who's color plays next
  def color = chess.Color.fromPly(ply)

  def toJson = Json toJson this

object Step:

  case class Move(uci: Uci, san: String):
    def uciString = uci.uci

  // TODO copied from lila.game
  // put all that shit somewhere else
  given OWrites[Crazyhouse.Pocket] = OWrites { v =>
    JsObject(
      Crazyhouse.storableRoles.flatMap { role =>
        Some(v.roles.count(role ==)).filter(0 <).map { count =>
          role.name -> JsNumber(count)
        }
      }
    )
  }
  given OWrites[chess.variant.Crazyhouse.Data] = OWrites { v =>
    Json.obj("pockets" -> List(v.pockets.white, v.pockets.black))
  }

  given Writes[Step] = Writes { step =>
    import lila.common.Json.given
    import step.*
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
