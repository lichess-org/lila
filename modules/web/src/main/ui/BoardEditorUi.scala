package lila.web
package ui

import chess.format.Fen
import play.api.libs.json._

import lila.common.Json.given
import lila.ui._

import ScalatagsTemplate._

final case class BoardEditorConfig(
  fen: Option[Fen.Full] = None,
  positionsJson: JsArray,
  endgamePositionsJson: JsArray
)

final class BoardEditorUi(helpers: Helpers):
  import helpers.{ *, given }

  def apply(config: BoardEditorConfig)(using Context) =
    Page(trans.site.boardEditor.txt())
      .js(
        PageModule(
          "editor",
          constructJsData(config.fen) ++ Json.obj(
            "positions" -> config.positionsJson,
            "endgamePositions" -> config.endgamePositionsJson
          )
        )
      )
      .css("editor")
      .zoom
      .graph(
        title = "Chess Board Editor",
        url = s"$netBaseUrl${routes.Editor.index.url}",
        description = "Load opening positions or create your own chess position on a chess board editor"
      ) {
        renderBoardEditor()
      }

  private def constructJsData(fen: Option[Fen.Full])(using ctx: Context) =
    Json.obj(
      "baseUrl"   -> s"$netBaseUrl${routes.Editor.index}",
      "animation" -> Json.obj("duration" -> ctx.pref.animationMillis),
      "is3d"      -> ctx.pref.is3d
    ) ++ fen.map("fen" -> _).toList

  private def renderBoardEditor() =
    main(id := "board-editor")(
      div(cls := "board-editor")(
        div(cls := "spare"),
        div(cls := "main-board")(chessgroundBoard),
        div(cls := "spare")
      )
    )
