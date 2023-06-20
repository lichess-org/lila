package lila.socket

import chess.format.{ Fen, Uci }
import chess.{ Check, Ply, Square }
import chess.variant.Crazyhouse
import play.api.libs.json.*

case class Step(
    ply: Ply,
    move: Option[Uci.WithSan],
    fen: Fen.Epd,
    check: Check,
    // None when not computed yet
    dests: Option[Map[Square, List[Square]]],
    drops: Option[List[Square]],
    crazyData: Option[Crazyhouse.Data]
):
  // who's color plays next
  def color = ply.turn

  def toJson = Json toJson this

object Step:

  import lila.common.Json.given

  given Writes[Step] = Writes { step =>
    import lila.common.Json.given
    import step.*
    Json
      .obj(
        "ply" -> ply,
        "uci" -> move.map(_.uci.uci),
        "san" -> move.map(_.san),
        "fen" -> fen
      )
      .add("check", check)
      .add(
        "dests",
        dests.map {
          _.map { case (orig, dests) =>
            s"${orig.asChar}${dests.map(_.asChar).mkString}"
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
