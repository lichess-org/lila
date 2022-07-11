package views.html.tutor

import controllers.routes
import play.api.libs.json._
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.Heapsort.implicits._
import lila.tutor.TutorCompare.comparisonOrdering
import lila.tutor.{ TutorFullReport, TutorPerfReport }
import lila.user.User

object perf {

  def apply(full: TutorFullReport.Available, report: TutorPerfReport, user: User)(implicit
      ctx: Context
  ) =
    bits.layout(full, menu = menu(full, user, report, "perf"))(
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
          chess.Color.all map { color =>
            report.openings(color).families.headOption map { fam =>
              grade.peerGrade(concept.adhoc(s"${fam.family.name} as $color"), fam.mix, h4)
            }
          }
        ),
        angleCard(
          routes.Tutor.time(user.username, report.perf.key),
          frag(report.perf.trans, " time management")
        )(
          grade.peerGrade(concept.speed, report.globalClock),
          grade.peerGrade(concept.clockFlagVictory, report.flagging.win),
          grade.peerGrade(concept.clockTimeUsage, report.clockUsage)
        ),
        angleCard(routes.Tutor.phases(user.username, report.perf.key), frag(report.perf.trans, " phases"))(
          report.phases.map { phase =>
            grade.peerGrade(concept.phase(phase.phase), phase.mix)
          }
        )
      )
    )

  private[tutor] def menu(
      full: TutorFullReport.Available,
      user: User,
      report: TutorPerfReport,
      active: String
  )(implicit
      ctx: Context
  ) = frag(
    a(href := routes.Tutor.perf(user.username, report.perf.key), cls := active.active("perf"))(
      report.perf.trans
    ),
    a(href := routes.Tutor.openings(user.username, report.perf.key), cls := active.active("openings"))(
      "Openings"
    ),
    a(href := routes.Tutor.time(user.username, report.perf.key), cls := active.active("time"))(
      "Time management"
    ),
    a(href := routes.Tutor.phases(user.username, report.perf.key), cls := active.active("phases"))(
      "Game phases"
    )
  )

  private def angleCard(url: Call, title: Frag)(content: Modifier*)(implicit ctx: Context) =
    st.article(cls := "tutor__perf__angle tutor-card tutor-card--link", dataHref := url)(
      div(cls := "tutor-card__top")(
        div(cls := "tutor-card__top__title tutor-card__top__title--pad")(
          h3(cls := "tutor-card__top__title__text")(title)
        )
      ),
      div(cls := "tutor-card__content")(content)
    )
}
