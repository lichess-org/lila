package views.html.tournament

import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue

object calendar:

  def apply(json: play.api.libs.json.JsObject)(using PageContext) =
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
