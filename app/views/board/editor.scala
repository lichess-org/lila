package views.html.board

import play.api.libs.json.JsObject

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
  )(implicit ctx: Context) = views.html.base.layout(
    title = trans.boardEditor.txt(),
    moreJs = frag(
      jsAt(s"compiled/lichess.editor${isProd ?? (".min")}.js"),
      embedJs(s"""var data=${safeJsonValue(bits.jsData(sit, fen, animationDuration))};data.positions=$positionsJson;
LichessEditor(document.getElementById('board_editor'), data);""")
    ),
    moreCss = cssTag("boardEditor.css"),
    chessground = false,
    openGraph = lila.app.ui.OpenGraph(
      title = "Chess board editor",
      url = s"$netBaseUrl${routes.Editor.index.url}",
      description = "Load opening positions or create your own chess position on a chess board editor"
    ).some
  ) {
      div(id := "board_editor", cls := "board_editor cg-512")
    }
}
