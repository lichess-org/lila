package views.html.tutor

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.Heapsort.implicits._
import lila.tutor.{ TutorCompare, TutorFullReport, TutorPerfReport }
import lila.tutor.TutorCompare.comparisonOrdering
import lila.user.User

object home {

  def apply(full: TutorFullReport.Available, user: User)(implicit ctx: Context) =
    bits.layout(full, menu = perf.menu(full, user, none))(
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
          ),
          p(
            "You particularly enjoy playing ",
            full.report.favouritePerfs.map(_.perf.trans).mkString(" and "),
            "!"
          ),
          p(
            h2("Your strengths:"),
            ul(full.report strengths 4 map { case (comp, perf) =>
              compare.showWithPerf(comp, perf.some)
            })
          ),
          p(
            h2("Your weaknesses:"),
            ul(full.report weaknesses 4 map { case (comp, perf) =>
              compare.showWithPerf(comp, perf.some)
            })
          )
        )
      },
      div(cls := "tutor__perfs tutor-cards")(
        full.report.perfs.toList map { perfReportCard(full.report, _, user) }
      )
    )

  private def perfReportCard(report: TutorFullReport, perfReport: TutorPerfReport, user: User)(implicit
      ctx: Context
  ) =
    st.article(cls := "tutor__perfs__perf tutor-card tutor-overlaid")(
      a(
        cls  := "tutor-overlay",
        href := routes.Tutor.perf(user.username, perfReport.perf.key)
      ),
      div(cls := "tutor-card__top")(
        iconTag(perfReport.perf.iconChar),
        div(cls := "tutor-card__top__title")(
          h3(cls := "tutor-card__top__title__text")(
            perfReport.stats.totalNbGames.localize,
            " ",
            perfReport.perf.trans,
            " games"
          ),
          report ratioTimeOf perfReport.perf map { ratio =>
            div(cls := "tutor-card__top__title__sub")(
              strong(ratio.percent.toInt, "%"),
              " of your chess playing time."
            )
          }
        )
      ),
      div(cls := "tutor-card__content")(
        bits.peerComparison("Accuracy", perfReport.accuracy),
        bits.peerComparison("Tactical Awareness", perfReport.awareness),
        bits.peerComparison("Time pressure", perfReport.globalTimePressure),
        bits.peerComparison("Clock flag victory", perfReport.flagging.win),
        ul(perfReport.relevantComparisons.topN(3) map compare.show),
        bits.seeMore
      )
    )
}
