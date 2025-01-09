package views.html.board

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object editor {

  def apply(
      sit: shogi.Situation,
      orientation: shogi.Color
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.boardEditor.txt(),
      moreCss = frag(
        cssTag("editor"),
        sit.variant.chushogi option chuPieceSprite,
        sit.variant.kyotoshogi option kyoPieceSprite
      ),
      moreJs = moduleJsTag("editor", jsData(sit, orientation)),
      shogiground = false,
      zoomable = true,
      openGraph = lila.app.ui
        .OpenGraph(
          title = trans.boardEditor.txt(),
          url = s"$netBaseUrl${routes.Editor.index.url}",
          description = trans.editorDescription.txt()
        )
        .some,
      canonicalPath = lila.common.CanonicalPath(routes.Editor.index).some,
      withHrefLangs = lila.i18n.LangList.All.some
    )(
      main(id := "editor-app")(
        div(cls   := s"board-editor ${mainVariantClass(sit.variant)}")(
          div(cls := "spare spare-top"),
          div(cls := s"main-board ${variantClass(sit.variant)}")(shogigroundEmpty(sit.variant, shogi.Sente)),
          div(cls := "spare spare-bottom"),
          div(cls := "actions"),
          div(cls := "links"),
          div(cls := "underboard")
        )
      )
    )

  def jsData(
      sit: shogi.Situation,
      orientation: shogi.Color
  )(implicit ctx: Context) =
    Json.obj(
      "sfen"    -> sit.toSfen.truncate.value,
      "variant" -> sit.variant.key,
      "baseUrl" -> netBaseUrl,
      "options" -> Json.obj(
        "orientation" -> orientation.name
      ),
      "pref" -> Json
        .obj(
          "animation"          -> ctx.pref.animationMillis,
          "coords"             -> ctx.pref.coords,
          "moveEvent"          -> ctx.pref.moveEvent,
          "resizeHandle"       -> ctx.pref.resizeHandle,
          "highlightLastDests" -> ctx.pref.highlightLastDests,
          "squareOverlay"      -> ctx.pref.squareOverlay
        )
    )

}
