package views.html.learn

import controllers.routes
import play.api.libs.json.Json
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object index {

  import trans.learn.{ play => _, _ }

  def apply(data: Option[JsValue], pref: lila.pref.Pref)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${learnShogi.txt()} - ${byPlaying.txt()}",
      moreCss = cssTag("learn"),
      moreJs = frag(
        translationJsTag("learn"),
        moduleJsTag(
          "learn",
          Json.obj(
            "data" -> data,
            "pref" -> Json.obj(
              "coords"             -> pref.coords,
              "moveEvent"          -> pref.moveEvent,
              "highlightLastDests" -> pref.highlightLastDests,
              "highlightCheck"     -> pref.highlightCheck,
              "squareOverlay"      -> pref.squareOverlay,
              "resizeHandle"       -> pref.resizeHandle
            )
          )
        )
      ),
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${learnShogi.txt()} - ${byPlaying.txt()}",
          description = s"${trans.learn.introBasics.txt()} ${trans.learn.introIntro.txt()}",
          url = s"$netBaseUrl${routes.Learn.index.url}"
        )
        .some,
      zoomable = true,
      shogiground = false,
      canonicalPath = lila.common.CanonicalPath(routes.Learn.index).some,
      withHrefLangs = lila.i18n.LangList.All.some
    ) {
      main(id   := "learn-app")(
        div(cls := "learn-app--wrap")
      )
    }

}
