package views.html.tutor

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json._
import scalatags.Text

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.insight.InsightPosition
import lila.tutor.{ Rating, TutorBothValueOptions, TutorFullReport, TutorNumber, ValueCount }

object bits {

  val mascot =
    img(
      cls := "mascot",
      src := assetUrl("images/mascot/octopus-shadow.svg")
    )

  def mascotSays(content: Modifier*) = div(cls := "mascot-says")(
    div(cls := "mascot-says__content")(content),
    mascot
  )

  val seeMore = a(cls := "tutor-card__more")("Click to see more...")

  def percentNumber[A](v: A)(implicit number: TutorNumber[A]) = f"${number double v}%1.1f"
  def percentFrag[A](v: A)(implicit number: TutorNumber[A])   = frag(strong(percentNumber(v)), "%")

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
      moreJs = jsModule("tutor"),
      title = "Lichess Tutor"
    ) {
      main(cls := List("page-menu tutor" -> true, "page-small" -> pageSmall))(
        st.aside(cls := "page-menu__menu subnav")(menu),
        div(cls := "page-menu__content")(content)
      )
    }
}
