package views.html

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User
import lila.racer.RacerRace

object racer {

  def home(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("racer")),
      title = "Puzzle Racer"
    ) {
      main(cls := "racer racer--home")(
        h1("Puzzle Racer"),
        div(
          postForm(cls := "racer__create", action := routes.Racer.create)(
            submitButton(cls := "button button-fat")("Race your friends")
          )
        )
      )
    }

  def show(race: RacerRace, data: JsObject, pref: JsObject)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("racer")),
      title = "Puzzle Racer",
      zoomable = true,
      chessground = false
    ) {
      main(
        div(cls := "racer racer--app")(race.toString)
      )
    }
}
