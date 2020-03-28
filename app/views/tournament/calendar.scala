package views.html.tournament

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object calendar {

  def apply(json: play.api.libs.json.JsObject)(implicit ctx: Context) = views.html.base.layout(
    title = "Tournament calendar",
    moreJs = frag(
      jsAt(s"compiled/lidraughts.tournamentCalendar${isProd ?? (".min")}.js"),
      embedJs(s"""LidraughtsTournamentCalendar.app(document.getElementById('tournament_calendar'), {
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
