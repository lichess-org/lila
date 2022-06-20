package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.TutorFullReport

object bits {

  val mascot =
    img(
      cls := "tutor__mascot",
      src := assetUrl("images/mascot/octopus-shadow.svg")
    )

  def mascotSays(content: Modifier*) = div(cls := "tutor__mascot-says")(
    div(cls := "tutor__mascot-says__content")(content),
    mascot
  )

  private[tutor] def layout(
      availability: TutorFullReport.Availability,
      menu: Frag,
      title: String = "Lichess Tutor",
      pageSmall: Boolean = false
  )(
      content: Modifier*
  )(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("tutor"),
      title = "Lichess Tutor"
    ) {
      main(cls := List("page-menu tutor" -> true, "page-small" -> pageSmall))(
        st.aside(cls := "page-menu__menu subnav")(menu),
        div(cls := "page-menu__content")(content)
      )
    }
}
