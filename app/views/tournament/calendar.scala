package views.html.tournament

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object calendar {

  def apply(json: play.api.libs.json.JsObject)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.tournamentCalendar.txt(),
      moreJs = frag(
        moduleJsTag(
          "tournament.calendar",
          Json.obj(
            "data" -> json,
          ),
        ),
      ),
      moreCss = cssTag("tournament.calendar"),
    ) {
      main(cls := "box")(
        h1(trans.tournamentCalendar()),
        div(id := "tournament-calendar"),
      )
    }
}
