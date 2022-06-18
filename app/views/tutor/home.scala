package views.html

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.tutor._

object tutor {

  def home(report: TutorFullReport)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("tutor")),
      title = "Lichess Tutor"
    ) {
      main(
        h1("Lichess Tutor"),
        div(cls := "tutor tutor__report")(
          chess.Color.all.map { color =>
            st.section(cls := "tutor__report__opeing")(
              h2(color.name),
              report.openings.colors(color).families.map { fam =>
                div(
                  h3(fam.family.name.value),
                  p("Rating gain: ", showMetric(fam.ratingGain)),
                  p("ACPL: ", showMetric(fam.acpl)),
                  p("Games: ", showMetric(fam.games))
                )
              }
            )
          }
        )
      )
    }

  private def showMetric[A](metric: TutorMetric[A]) =
    div(cls := "metric")(
      strong(metricValue(metric.mine)),
      em(" (peers: ", metricValue(metric.peer), ")")
    )

  private def metricValue[A](value: A) = value match {
    case TutorRatio(v)         => f"${v * 100}%1.1f%%"
    case v: Double if v >= 100 => f"$v%1.0f"
    case v: Double             => f"$v%1.1f"
    case v                     => v.toString
  }

  sealed trait MetricType
  case object Percent extends MetricType
}
