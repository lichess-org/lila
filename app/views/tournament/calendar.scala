package views.html.tournament

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment.*
import lila.app.ui.ScalatagsTemplate.*
import lila.common.String.html.safeJsonValue

object calendar:

  def apply(json: play.api.libs.json.JsObject)(using Context) =
    views.html.base.layout(
      title = "Tournament calendar",
      moreJs = frag(
        jsModule("tournament.calendar"),
        embedJsUnsafeLoadThen(
          s"""LichessTournamentCalendar.app(document.getElementById('tournament-calendar'), ${safeJsonValue(
              Json.obj("data" -> json)
            )})"""
        )
      ),
      moreCss = cssTag("tournament.calendar")
    ) {
      main(cls := "box")(
        h1(cls := "box__top")("Tournament calendar"),
        div(id := "tournament-calendar")
      )
    }
