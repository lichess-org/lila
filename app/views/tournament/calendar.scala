package views.html.tournament

import play.api.libs.json.Json
import play.api.i18n.Lang

import lila.app.templating.Environment.*
import lila.app.ui.ScalatagsTemplate.*
import lila.common.String.html.safeJsonValue

object calendar:

  def apply(json: play.api.libs.json.JsObject)(using Lang) =
    views.html.base.layout(
      title = "Tournament calendar",
      moreJs = jsModuleInit("tournament.calendar", Json.obj("data" -> json)),
      moreCss = cssTag("tournament.calendar")
    ) {
      main(cls := "box")(
        h1(cls := "box__top")(trans.tournamentCalendar()),
        div(id := "tournament-calendar")
      )
    }
