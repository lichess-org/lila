package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{
  Rating,
  TutorFullReport,
  TutorMetric,
  TutorMetricOption,
  TutorNumber,
  TutorRatio,
  ValueComparison,
  ValueCount
}

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

  val seeMore = a(cls := "tutor-card__more")("Click to see more...")

  def peerComparison[A](name: String, metric: TutorMetricOption[A])(implicit number: TutorNumber[A]) =
    metric.mine map { mine =>
      div(cls := "tutor-comparison")(
        h3(cls := "tutor-comparison__name")(name),
        div(cls                                                             := "tutor-comparison__unit")(
          horizontalBarPercent(number.iso.to(mine.value).some, "Yours")(cls := "tutor-bar--mine")
        ),
        div(cls := "tutor-comparison__unit")(
          horizontalBarPercent(metric.peer.map(_.value).map(number.iso.to), "Peers")(cls := "tutor-bar--peer")
        )
      )
    }

  private def horizontalBarPercent(value: Option[Double], legend: String) =
    value match {
      case Some(v) =>
        div(cls := "tutor-bar", style := s"--value:${Math.round(v)}%")(
          span(legend),
          em(strong(f"$v%1.1f"), "%")
        )
      case None => div(cls := "tutor-bar tutor-bar--empty")
    }

  private[tutor] def layout(
      availability: TutorFullReport.Availability,
      menu: Frag,
      title: String = "Lichess Tutor",
      pageSmall: Boolean = false,
      moreJs: Frag = emptyFrag
  )(
      content: Modifier*
  )(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("tutor"),
      moreJs = moreJs,
      title = "Lichess Tutor"
    ) {
      main(cls := List("page-menu tutor" -> true, "page-small" -> pageSmall))(
        st.aside(cls := "page-menu__menu subnav")(menu),
        div(cls := "page-menu__content")(content)
      )
    }
}
