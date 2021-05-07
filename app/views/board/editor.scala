package views.html.board

import chess.format.FEN
import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

object editor {

  def apply(
      sit: chess.Situation,
      fen: FEN,
      positionsJson: String
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.boardEditor.txt(),
      moreJs = frag(
        jsModule("editor"),
        embedJsUnsafeLoadThen(
          s"""const data=${safeJsonValue(bits.jsData(sit, fen))};data.positions=$positionsJson;
LichessEditor(document.getElementById('board-editor'), data);"""
        )
      ),
      moreCss = cssTag("editor"),
      chessground = false,
      zoomable = true,
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Chess board editor",
          url = s"$netBaseUrl${routes.Editor.index.url}",
          description = "Load opening positions or create your own chess position on a chess board editor"
        )
        .some
    )(
      main(id := "board-editor")(
        div(cls := "board-editor")(
          div(cls := "spare"),
          div(cls := "main-board")(chessgroundBoard),
          div(cls := "spare")
        )
      )
    )
}
