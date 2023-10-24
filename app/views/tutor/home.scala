package views.html.tutor

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.tutor.{ TutorFullReport, TutorPeriodReport }
import lila.user.User

object home:

  def apply(reports: TutorPeriodReport.UserReports)(using PageContext) =
    import reports.user
    bits.layout(menu = menu(reports, none))(
      cls := "tutor__home box",
      boxTop(h1(bits.otherUser(user), "Lichess Tutor")),
      "something here?"
      // if full.report.perfs.isEmpty then empty.mascotSaysInsufficient
      // else
      //   bits.mascotSays(
      //     p(
      //       strong(
      //         cls := "tutor__intro",
      //         "Hello, I have examined ",
      //         full.report.nbGames.localize,
      //         " recent rated games of yours."
      //       )
      //     ),
      //     p("Let's compare your play style to your peers: players with a rating very similar to yours."),
      //     p(
      //       "It should give us some idea about what your strengths are, and where you have room for improvement."
      //     )
      //   )
      // ,
      // div(cls := "tutor__perfs tutor-cards")(
      //   full.report.perfs.toList map { reportCard(full.report, _, user) }
      // )
    )

  private[tutor] def menu(reports: TutorPeriodReport.UserReports, report: Option[TutorPeriodReport])(using
      PageContext
  ) = frag(
    a(href := routes.Tutor.user(reports.user.username), cls := report.isEmpty.option("active"))("Tutor"),
    reports.next.map: r =>
      a(
        cls  := List("tutor-report tutor-report--next" -> true),
        href := routes.Tutor.perf(reports.user.username, r.perf.key, r.reportId)
      )(r.query.toString),
    reports.past.map: r =>
      a(
        cls  := List("tutor-report tutor-report--past" -> true, "active" -> report.exists(_.id == r.id)),
        href := routes.Tutor.perf(reports.user.username, r.perf.key, r.id)
      )(r.perf.trans)
  )

  // private def reportCard(report: TutorFullReport, perfReport: TutorPeriodReport, user: User)(using
  //     PageContext
  // ) =
  //   st.article(
  //     cls      := "tutor__perfs__perf tutor-card tutor-card--link",
  //     dataHref := routes.Tutor.perf(user.username, report.perf.key)
  //   )(
  //     div(cls := "tutor-card__top")(
  //       iconTag(report.perf.icon),
  //       div(cls := "tutor-card__top__title")(
  //         h3(cls := "tutor-card__top__title__text")(
  //           report.stats.totalNbGames.localize,
  //           " ",
  //           report.perf.trans,
  //           " games"
  //         ),
  //         div(cls := "tutor-card__top__title__sub")(
  //           perf.timePercentAndRating(report, report)
  //         )
  //       )
  //     ),
  //     div(cls := "tutor-card__content")(
  //       grade.peerGrade(concept.accuracy, report.accuracy),
  //       grade.peerGrade(concept.tacticalAwareness, report.awareness),
  //       grade.peerGrade(concept.resourcefulness, report.resourcefulness),
  //       grade.peerGrade(concept.conversion, report.conversion),
  //       grade.peerGrade(concept.speed, report.globalClock),
  //       grade.peerGrade(concept.clockFlagVictory, report.flagging.win),
  //       grade.peerGrade(concept.clockTimeUsage, report.clockUsage),
  //       report.phases.map: phase =>
  //         grade.peerGrade(concept.phase(phase.phase), phase.mix),
  //       bits.seeMore
  //     )
  //   )
