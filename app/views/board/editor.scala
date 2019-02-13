package views.html.board

import play.api.libs.json.JsObject

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object editor {

  def apply(
    sit: draughts.Situation,
    fen: String,
    positionsJson: String,
    animationDuration: scala.concurrent.duration.Duration
  )(implicit ctx: Context) = views.html.base.layout(
    title = trans.boardEditor.txt(),
    moreJs = frag(
      jsAt(s"compiled/lidraughts.editor${isProd ?? (".min")}.js"),
      embedJs(s"""var data=${safeJsonValue(bits.jsData(sit, fen, animationDuration))};data.positions=$positionsJson;${isGranted(_.CreatePuzzles).??("data.puzzleEditor = true;")}
LidraughtsEditor(document.getElementById('board-editor'), data);""")
    ),
    moreCss = responsiveCssTag("editor"),
    draughtsground = false,
    zoomable = true,
    responsive = true,
    openGraph = lidraughts.app.ui.OpenGraph(
      title = "Draughts board editor",
      url = s"$netBaseUrl${routes.Editor.index.url}",
      description = "Load opening positions or create your own draughts position on a draughts board editor"
    ).some
  )(main(id := "board-editor")(
      div(cls := "spare"),
      draughtsgroundSvg,
      div(cls := "spare")
    ))
}
