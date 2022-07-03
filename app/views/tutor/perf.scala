package views.html.tutor

import controllers.routes
import play.api.libs.json._
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.Heapsort.implicits._
import lila.tutor.TutorCompare.comparisonOrdering
import lila.tutor.{ TutorFullReport, TutorMetric, TutorMetricOption, TutorPerfReport, TutorRatio }
import lila.user.User

object perf {

  def apply(full: TutorFullReport.Available, report: TutorPerfReport, user: User)(implicit
      ctx: Context
  ) =
    bits.layout(full, menu = menu(full, user, report.some))(
      cls := "tutor__perf box",
      h1(
        a(href := routes.Tutor.user(user.username), dataIcon := "", cls := "text"),
        "Tutor: ",
        report.perf.trans
      ),
      bits.mascotSays(ul(report.relevantComparisons.topN(3) map compare.show)),
      div(cls := "tutor__perf__angles tutor-cards")(
        angleCard(
          routes.Tutor.openings(user.username, report.perf.key),
          frag(report.perf.trans, " openings")
        )(
          ul(report openingHighlights 3 map compare.show)
        ),
        angleCard(
          routes.Tutor.time(user.username, report.perf.key),
          frag(report.perf.trans, " time management")
        )(
          ul(report timeHighlights 3 map compare.show)
        ),
        angleCard(routes.Tutor.phases(user.username, report.perf.key), frag(report.perf.trans, " phases"))(
          ul(report phaseHighlights 3 map compare.show)
        )
      )
    )

  private[tutor] def menu(full: TutorFullReport.Available, user: User, report: Option[TutorPerfReport])(
      implicit ctx: Context
  ) = frag(
    a(href := routes.Tutor.user(user.username), cls := report.isEmpty.option("active"))("Tutor"),
    full.report.perfs.map { p =>
      a(
        cls  := p.perf.key.active(report.??(_.perf.key)),
        href := routes.Tutor.perf(user.username, p.perf.key)
      )(p.perf.trans)
    }
  )

  private def angleCard(url: Call, title: Frag)(content: Modifier*)(implicit ctx: Context) =
    st.article(cls := "tutor__perf__angle tutor-card tutor-overlaid")(
      a(
        cls  := "tutor-overlay",
        href := url
      ),
      div(cls := "tutor-card__top")(
        div(cls := "tutor-card__top__title tutor-card__top__title--pad")(
          h3(cls := "tutor-card__top__title__text")(title)
        )
      ),
      div(cls := "tutor-card__content")(content)
    )
}
