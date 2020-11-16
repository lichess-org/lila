package views.html.board

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object editor {

  def apply(
      sit: chess.Situation,
      fen: String,
      positionsJson: String,
      animationDuration: scala.concurrent.duration.Duration
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.boardEditor.txt(),
      moreJs = frag(
        jsAt(s"compiled/lishogi.editor${isProd ?? ".min"}.js"),
        embedJsUnsafe(
          s"""var data=${safeJsonValue(bits.jsData(sit, fen, animationDuration))};data.positions=$positionsJson;
LishogiEditor(document.getElementById('board-editor'), data);"""
        )
      ),
      moreCss = cssTag("editor"),
      shogiground = false,
      zoomable = true,
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Shogi board editor",
          url = s"$netBaseUrl${routes.Editor.index().url}",
          description = "Load opening positions or create your own shogi position on a shogi board editor"
        )
        .some
    )(
      main(id := "board-editor")(
        div(cls := "board-editor")(
          div(cls := "spare"),
          div(cls := "main-board")(shogigroundBoard),
          div(cls := "spare")
        )
      )
    )
}
