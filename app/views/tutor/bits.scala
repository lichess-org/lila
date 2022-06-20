package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User
import lila.tutor.TutorReport

object bits {

  private[tutor] def layout(
      availability: TutorReport.Availability,
      menu: Frag,
      title: String = "Lichess Tutor"
  )(
      content: Modifier*
  )(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("tutor"),
      title = "Lichess Tutor"
    ) {
      main(cls := "page-menu tutor")(
        st.aside(cls := "page-menu__menu subnav")(menu),
        div(cls := "page-menu__content")(content)
      )
    }

  def empty(availability: TutorReport.Empty, user: User)(implicit ctx: Context) =
    layout(availability, menu = emptyFrag)(
      cls := "tutor__insufficient box box-pad",
      h1("Lichess Tutor"),
      p("Explain what tutor is about here."),
      postForm(action := routes.Tutor.refresh(user.username))(
        submitButton(cls := "button button-fat")("Analyse my games and help me improve")
      )
    )

  def insufficientGames(implicit ctx: Context) =
    layout(TutorReport.InsufficientGames, menu = emptyFrag)(
      cls := "tutor__insufficient box box-pad",
      h1("Lichess Tutor"),
      p("Not enough games to analyse! Go and play some more chess.")
    )
}
