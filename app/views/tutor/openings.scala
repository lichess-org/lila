package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.tutor.{ TutorMetric, TutorMetricOption, TutorPerfReport, TutorRatio, TutorReport }

object openings {

  def apply(fullReport: TutorReport, report: TutorPerfReport, user: lila.user.User)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("tutor")),
      title = "Lichess Tutor"
    ) {
      main(cls := "page-menu tutor")(
        st.aside(cls := "page-menu__menu subnav")(
          a(href := routes.Tutor.user(user.username))("Tutor"),
          a(href := routes.Tutor.openings(user.username, report.perf.key), cls := "active")("Openings"),
          a(href := routes.Tutor.phases(user.username, report.perf.key))("Game phases")
        ),
        div(cls := "page-menu__content box box-pad")(
          h1(
            a(href := routes.Tutor.perf(user.username, report.perf.key), dataIcon := "", cls := "text"),
            report.perf.trans,
            " openings"
          ),
          div(cls := "tutor__openings")(
            chess.Color.all.map { color =>
              st.section(cls := "tutor__openings__color")(
                h2("Your most played ", color.name, " openings"),
                report.openings(color).families.map { fam =>
                  div(cls := "tutor__opening tutor-card tutor-overlaid")(
                    h3(fam.family.name.value),
                    table(cls := "slist")(
                      thead(tr(th("Metric"), th("You"), th("Peers"))),
                      tbody(
                        tr(
                          th("Frequency"),
                          showMetric(fam.games, none)
                        ),
                        tr(
                          th("Performance"),
                          showMetric(fam.performance, true.some)
                        ),
                        tr(
                          th("Tactical awareness"),
                          showMetric(fam.awareness, true.some)
                        ),
                        tr(
                          th("Centipawn loss"),
                          showMetric(fam.acpl, false.some)
                        )
                      )
                    )
                  )
                }
              )
            }
          )
        )
      )
    }

  private def qualityCls(higherIsBetter: Option[Boolean], higher: Boolean) = higherIsBetter map { hib =>
    if (hib == higher) "good" else "bad"
  }

  private def showMetric[A](metric: TutorMetric[A], higherIsBetter: Option[Boolean]) =
    frag(
      td(cls := qualityCls(higherIsBetter, metric.higher))(metricValue(metric.mine)),
      td(metric.peer.fold("?")(metricValue))
    )

  private def showMetric[A](metric: TutorMetricOption[A], higherIsBetter: Option[Boolean]) =
    frag(
      td(cls := qualityCls(higherIsBetter, metric.higher))(metric.mine.fold("?")(metricValue)),
      td(metric.peer.fold("?")(metricValue))
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
