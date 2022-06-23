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

  // def qualityCls(higherIsBetter: Option[Boolean], higher: Boolean) = higherIsBetter map { hib =>
  //   if (hib == higher) "good" else "bad"
  // }

  // def showCount[A](metric: TutorMetric[A]) =
  //   frag(
  //     td(metric.mine.count),
  //     td(metric.peer.fold("?")(_.count.toString))
  //   )

  // def showMetric[A](metric: TutorMetric[A], higherIsBetter: Option[Boolean]) =
  //   frag(
  //     td(cls := qualityCls(higherIsBetter, metric.higher))(metricValue(metric.mine)),
  //     td(metric.peer.fold("?")(metricValue))
  //   )

  // def showMetric[A](metric: TutorMetricOption[A], higherIsBetter: Option[Boolean]) =
  //   frag(
  //     td(cls := qualityCls(higherIsBetter, metric.higher))(metric.mine.fold("?")(metricValue)),
  //     td(metric.peer.fold("?")(metricValue))
  //   )

  // def metricValue[A](value: ValueCount[A]): String = metricValue(value.value)

  // def metricValue[A](value: A): String = value match {
  //   case TutorRatio(v)         => f"${v * 100}%1.1f%%"
  //   case Rating(v)             => f"$v%1.0f"
  //   case v: Double if v >= 100 => f"$v%1.0f"
  //   case v: Double             => f"$v%1.1f"
  //   case v                     => v.toString
  // }

  // sealed trait MetricType
  // case object Percent extends MetricType

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
