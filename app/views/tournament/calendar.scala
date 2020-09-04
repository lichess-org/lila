package views.html.tournament

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

object calendar {

  def apply(json: play.api.libs.json.JsObject)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Tournament calendar",
      moreJs = frag(
        jsModule("tournament.calendar"),
        embedJsUnsafeLoadThen(
          s"""LichessTournamentCalendar.app(document.getElementById('tournament-calendar'), ${safeJsonValue(
            Json.obj(
              "data" -> json,
              "i18n" -> bits.jsI18n
            )
          )})"""
        )
      ),
      moreCss = cssTag("tournament.calendar")
    ) {
      main(cls := "box")(
        h1("Tournament calendar"),
        div(id := "tournament-calendar")
      )
    }
}
