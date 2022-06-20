package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ RelativeQuality, TutorCompare, TutorFullReport, TutorPerfReport }
import lila.user.User

object home {

  def apply(full: TutorFullReport.Available, user: User)(implicit ctx: Context) =
    bits.layout(full, menu = perf.menu(full, user, none))(
      cls := "tutor__home box",
      h1("Lichess Tutor"),
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
        p("Let's see how to improve.")
      ),
      div(cls := "tutor__perfs")(
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
            perfReport.stats.nbGames.localize,
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
        perfReport.dimensionHighlights(2) map showHighlight,
        perfReport.peerHighlights(2) map showHighlight
      )
    )

  private def showHighlight(comp: TutorCompare.Comparison[_, _]) =
    p(
      "Your ",
      comp.metricType.toString,
      " in the ",
      comp.dimension.toString,
      " is ",
      showQuality(comp.quality),
      " than ",
      comp.reference match {
        case TutorCompare.OtherDim(dim, _) => frag("in the ", dim.toString)
        case TutorCompare.Peers(_)         => frag("your peers'")
      }
    )

  private def showQuality(quality: RelativeQuality) =
    (if (quality.positive) goodTag else badTag)(quality.wording.value)

  // p(comp.quality.wording.toString, " ", comp.toString)
}
