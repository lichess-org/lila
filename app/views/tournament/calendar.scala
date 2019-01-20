package views.html.tournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object calendar {

  def apply(json: play.api.libs.json.JsObject)(implicit ctx: Context) = views.html.base.layout(
    title = "Tournament calendar",
    moreJs = frag(
      jsAt(s"compiled/lichess.tournamentCalendar${isProd ?? (".min")}.js"),
      embedJs(s"""LichessTournamentCalendar.app(document.getElementById('tournament_calendar'), {
data: ${safeJsonValue(json)},
i18n: ${jsI18n()}
});""")
    ),
    moreCss = cssTag("tournament_calendar.css")
  ) {
      div(cls := "content_box no_padding tournament_calendar")(
        h1("Tournament calendar"),
        div(id := "tournament_calendar")
      )
    }
}
