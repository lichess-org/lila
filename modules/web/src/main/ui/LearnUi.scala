package lila.web
package ui

import play.api.libs.json.Json

import lila.ui.*

import ScalatagsTemplate.*

final class LearnUi(helpers: Helpers):
  import helpers.{ *, given }
  import trans.learn as trl

  def apply(data: Option[play.api.libs.json.JsValue])(using ctx: Context) =
    Page(s"${trl.learnChess.txt()} - ${trl.byPlaying.txt()}")
      .js:
        PageModule(
          "learn",
          Json.obj(
            "data" -> data,
            "pref" -> Json.obj(
              "coords" -> ctx.pref.coords,
              "destination" -> ctx.pref.destination,
              "is3d" -> ctx.pref.is3d
            )
          )
        )
      .css("learn")
      .i18n(_.learn)
      .graph(
        title = "Learn chess by playing",
        description = "You don't know much about chess? Excellent! Let's have fun and learn to play chess!",
        url = s"$netBaseUrl${routes.Learn.index}"
      )
      .hrefLangs(lila.ui.LangPath(routes.Learn.index))
      .flag(_.zoom):
        main(id := "learn-app")
