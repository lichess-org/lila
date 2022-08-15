package views.html.board

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object editor {

  def apply(
      sit: shogi.Situation,
      positionsJson: String
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.boardEditor.txt(),
      moreJs = frag(
        jsModule("editor"),
        embedJsUnsafe(
          s"""var data=${safeJsonValue(jsData(sit))};data.positions=$positionsJson;
LishogiEditor(document.getElementById('board-editor'), data);"""
        )
      ),
      moreCss = cssTag("editor"),
      shogiground = false,
      zoomable = true,
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Shogi board editor",
          url = s"$netBaseUrl${routes.Editor.index.url}",
          description = "Load opening positions or create your own shogi position on a shogi board editor"
        )
        .some
    )(
      main(id := "board-editor")(
        div(cls   := s"board-editor variant-${sit.variant.key}")(
          div(cls := "spare"),
          div(cls := "main-board")(shogigroundBoard(sit.variant, shogi.Sente.some)),
          div(cls := "spare")
        )
      )
    )

  def jsData(
      sit: shogi.Situation
  )(implicit ctx: Context) =
    Json.obj(
      "sfen"    -> sit.toSfen.truncate.value,
      "variant" -> sit.variant.key,
      "baseUrl" -> s"$netBaseUrl${routes.Editor.index}",
      "pref" -> Json
        .obj(
          "animation"          -> ctx.pref.animationMillis,
          "coords"             -> ctx.pref.coords,
          "moveEvent"          -> ctx.pref.moveEvent,
          "resizeHandle"       -> ctx.pref.resizeHandle,
          "highlightLastDests" -> ctx.pref.highlightLastDests,
          "squareOverlay"      -> ctx.pref.squareOverlay,
          "notation"           -> ctx.pref.notation
        ),
      "i18n" -> i18nJsObject(i18nKeyes)
    )

  private val i18nKeyes = List(
    trans.setTheBoard,
    trans.boardEditor,
    trans.startPosition,
    trans.clearBoard,
    trans.fillGotesHand,
    trans.flipBoard,
    trans.loadPosition,
    trans.popularOpenings,
    trans.handicaps,
    trans.whitePlays,
    trans.blackPlays,
    trans.uwatePlays,
    trans.shitatePlays,
    trans.variant,
    trans.continueFromHere,
    trans.playWithTheMachine,
    trans.playWithAFriend,
    trans.analysis,
    trans.toStudy
  ).map(_.key)
}
