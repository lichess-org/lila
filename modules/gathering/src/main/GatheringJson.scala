package lila.gathering

import play.api.libs.json.*
import chess.format.Fen
import lila.common.Json.given

object GatheringJson:

  def position(fen: Fen.Standard): JsObject =
    Thematic.byFen(fen) match
      case Some(pos) =>
        Json
          .obj(
            "eco"  -> pos.eco,
            "name" -> pos.name,
            "fen"  -> pos.fen,
            "url"  -> pos.url
          )
      case None =>
        Json
          .obj(
            "name" -> "Custom position",
            "fen"  -> fen
          )
