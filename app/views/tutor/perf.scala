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
              bits.peerGrade(concept.adhoc(s"${fam.family.name} as $color"), fam.mix, h4)
            }
          }
        ),
        angleCard(
          routes.Tutor.time(user.username, report.perf.key),
          frag(report.perf.trans, " time management")
        )(
          bits.peerGrade(concept.speed, report.globalClock),
          bits.peerGrade(concept.clockFlagVictory, report.flagging.win),
          bits.peerGrade(concept.clockTimeUsage, report.clockUsage)
        ),
        angleCard(routes.Tutor.phases(user.username, report.perf.key), frag(report.perf.trans, " phases"))(
          report.phases.map { phase =>
            bits.peerGrade(concept.adhoc(phase.phase.name), phase.mix)
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
  ) = {
    def subItem(key: String, name: String, url: Call) =
      a(href := url, cls := List("subnav__subitem" -> true, "active" -> (key == active)))(name)
    frag(
      a(href := routes.Tutor.user(user.username))("Tutor"),
      a(href := routes.Tutor.perf(user.username, report.perf.key), cls := "active")(report.perf.trans),
      subItem("time", "Time management", routes.Tutor.time(user.username, report.perf.key)),
      subItem("phases", "Game phases", routes.Tutor.phases(user.username, report.perf.key)),
      subItem("openings", "Openings", routes.Tutor.openings(user.username, report.perf.key))
    )
  }

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
