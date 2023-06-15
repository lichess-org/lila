package views.html.tournament

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

object calendar {

  def apply(json: play.api.libs.json.JsObject)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.tournamentCalendar.txt(),
      moreJs = frag(
        jsModule("tournamentCalendar"),
        embedJsUnsafe(
          s"""LishogiTournamentCalendar.app(document.getElementById('tournament-calendar'), ${safeJsonValue(
              Json.obj("data" -> json)
            )})"""
        )
      ),
      moreCss = cssTag("tournament.calendar")
    ) {
      main(cls := "box")(
        h1(trans.tournamentCalendar()),
        div(id := "tournament-calendar")
      )
    }
}
