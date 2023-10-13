package views.html.tutor

import controllers.routes
import play.api.mvc.Call

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.Heapsort.topN
import lila.tutor.{ TutorCompare, TutorPerfReport, TutorFullReport }
import lila.tutor.TutorCompare.given
import lila.user.User
import lila.rating.PerfType

object perf:

  def apply(full: TutorFullReport, report: TutorPerfReport, user: User)(using PageContext) =
    bits.layout(menu = menu(user, report, "perf"))(
      cls := "tutor__perf box",
      boxTop(
        h1(
          a(href := routes.Tutor.user(user.username), dataIcon := licon.LessThan, cls := "text"),
          bits.otherUser(user),
          "Tutor: ",
          report.perf.trans
        )
      ),
      bits.mascotSays(
        p(
          "Looking at ",
          pluralizeLocalize("game", report.stats.totalNbGames),
          report.stats.dates.map: dates =>
            frag(" played between ", showDate(dates.start), " and ", showDate(dates.end))
        ),
        timePercentAndRating(full, report),
        ul(TutorCompare.mixedBag(report.relevantComparisons)(4) map compare.show)
      ),
      div(cls := "tutor__perf__angles tutor-cards")(
        angleCard(
          frag(report.perf.trans, " skills"),
          routes.Tutor.skills(user.username, report.perf.key).some
        )(
          grade.peerGrade(concept.accuracy, report.accuracy),
          grade.peerGrade(concept.tacticalAwareness, report.awareness),
          grade.peerGrade(concept.resourcefulness, report.resourcefulness),
          grade.peerGrade(concept.conversion, report.conversion)
        ),
        angleCard(
          frag(report.perf.trans, " openings"),
          routes.Tutor.openings(user.username, report.perf.key).some
        )(
          chess.Color.all.map: color =>
            report.openings(color).families.headOption map { fam =>
              grade.peerGrade(concept.adhoc(s"${fam.family.name} as $color"), fam.mix, h4)
            }
        ),
        angleCard(
          frag(report.perf.trans, " time management"),
          report.perf != PerfType.Correspondence option routes.Tutor.time(user.username, report.perf.key)
        )(
          if report.perf == PerfType.Correspondence then p("Not applicable.")
          else
            frag(
              grade.peerGrade(concept.speed, report.globalClock),
              grade.peerGrade(concept.clockFlagVictory, report.flagging.win),
              grade.peerGrade(concept.clockTimeUsage, report.clockUsage)
            )
        ),
        angleCard(
          frag(report.perf.trans, " phases"),
          routes.Tutor.phases(user.username, report.perf.key).some
        ):
          report.phases.map: phase =>
            grade.peerGrade(concept.phase(phase.phase), phase.mix)
      )
    )

  private[tutor] def timePercentAndRating(
      report: TutorFullReport,
      perfReport: TutorPerfReport
  )(using Context) = p(
    report percentTimeOf perfReport.perf map { percent =>
      frag(
        perfReport.perf.trans,
        " games represent ",
        bits.percentFrag(percent),
        " of your chess playing time.",
        br
      )
    },
    frag(
      "Average rating: ",
      strong(perfReport.stats.rating),
      ". Peers rating: ",
      strong(perfReport.stats.peers.showRatingRange)
    )
  )

  private[tutor] def menu(
      user: User,
      report: TutorPerfReport,
      active: String
  )(using Context) = frag(
    a(href := routes.Tutor.perf(user.username, report.perf.key), cls := active.active("perf"))(
      report.perf.trans
    ),
    a(href := routes.Tutor.skills(user.username, report.perf.key), cls := active.active("skills"))(
      "Skills"
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

  private def angleCard(title: Frag, url: Option[Call])(content: Modifier*) =
    st.article(
      cls      := List("tutor__perf__angle tutor-card" -> true, "tutor-card--link" -> url.isDefined),
      dataHref := url
    )(
      div(cls := "tutor-card__top")(
        div(cls := "tutor-card__top__title tutor-card__top__title--pad")(
          h3(cls := "tutor-card__top__title__text")(title)
        )
      ),
      div(cls := "tutor-card__content")(content)
    )
