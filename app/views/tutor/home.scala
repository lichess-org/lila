package views.html.tutor

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.Heapsort.implicits._
import lila.tutor.{ TutorCompare, TutorFullReport, TutorPerfReport }
import lila.tutor.TutorCompare.comparisonOrdering
import lila.user.User
import lila.insight.Phase

object home {

  def apply(full: TutorFullReport.Available, user: User)(implicit ctx: Context) =
    bits.layout(full, menu = menu(full, user, none))(
      cls := "tutor__home box",
      h1("Lichess Tutor"),
      if (full.report.perfs.isEmpty) empty.mascotSaysInsufficient
      else {
        bits.mascotSays(
          p(
            strong(
              cls := "tutor__intro",
              "Hello, I have examined ",
              full.report.nbGames.localize,
              " recent rated games of yours."
            )
          )
        )
      },
      div(cls := "tutor__perfs tutor-cards")(
        full.report.perfs.toList map { perfReportCard(full.report, _, user) }
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

  private def perfReportCard(report: TutorFullReport, perfReport: TutorPerfReport, user: User)(implicit
      ctx: Context
  ) =
    st.article(
      cls      := "tutor__perfs__perf tutor-card tutor-card--link",
      dataHref := routes.Tutor.perf(user.username, perfReport.perf.key)
    )(
      div(cls := "tutor-card__top")(
        iconTag(perfReport.perf.iconChar),
        div(cls := "tutor-card__top__title")(
          h3(cls := "tutor-card__top__title__text")(
            perfReport.stats.totalNbGames.localize,
            " ",
            perfReport.perf.trans,
            " games"
          ),
          report percentTimeOf perfReport.perf map { percent =>
            div(cls := "tutor-card__top__title__sub")(
              bits.percentFrag(percent),
              " of your chess playing time."
            )
          }
        )
      ),
      div(cls := "tutor-card__content")(
        grade.peerGrade(concept.accuracy, perfReport.accuracy),
        grade.peerGrade(concept.tacticalAwareness, perfReport.awareness),
        grade.peerGrade(concept.speed, perfReport.globalClock),
        grade.peerGrade(concept.clockFlagVictory, perfReport.flagging.win),
        grade.peerGrade(concept.clockTimeUsage, perfReport.clockUsage),
        perfReport.phases.map { phase =>
          grade.peerGrade(concept.phase(phase.phase), phase.mix)
        },
        bits.seeMore
      )
    )
}
