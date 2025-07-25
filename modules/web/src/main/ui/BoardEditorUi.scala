package lila.web
package ui

import chess.format.Fen
import play.api.libs.json.*

import lila.common.Json.given
import lila.ui.*

import ScalatagsTemplate.*

final class BoardEditorUi(helpers: Helpers):
  import helpers.{ *, given }

  def apply(
      fen: Option[Fen.Full],
      positionsJson: JsArray,
      endgamePositionsJson: JsArray
  )(using Context) =
    Page(trans.site.boardEditor.txt())
      .js(
        PageModule(
          "editor",
          jsData(fen) ++ Json.obj("positions" -> positionsJson, "endgamePositions" -> endgamePositionsJson)
        )
      )
      .css("editor")
      .flag(_.zoom)
      .graph(
        title = "Chess board editor",
        url = s"$netBaseUrl${routes.Editor.index.url}",
        description = "Load opening positions or create your own chess position on a chess board editor"
      ):
        main(id := "board-editor")(
          div(cls := "board-editor")(
            div(cls := "spare"),
            div(cls := "main-board")(chessgroundBoard),
            div(cls := "spare")
          )
        )

  def jsData(fen: Option[Fen.Full] = None)(using ctx: Context) =
    Json
      .obj(
        "baseUrl" -> s"$netBaseUrl${routes.Editor.index}",
        "animation" -> Json.obj("duration" -> ctx.pref.animationMillis),
        "is3d" -> ctx.pref.is3d
      )
      .add("fen" -> fen)
