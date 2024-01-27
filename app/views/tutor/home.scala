package views.html.tutor

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.tutor.{ TutorFullReport, TutorPerfReport }
import lila.user.User

object home:

  def apply(full: TutorFullReport.Available, user: User)(using PageContext) =
    bits.layout(menu = menu(full, user, none))(
      cls := "tutor__home box",
      boxTop(h1(bits.otherUser(user), "Lichess Tutor")),
      if full.report.perfs.isEmpty then empty.mascotSaysInsufficient
      else
        bits.mascotSays(
          p(
            strong(
              cls := "tutor__intro",
              "Hello, I have examined ",
              full.report.nbGames.localize,
              " recent rated games of yours."
            )
          ),
          p("Let's compare your play style to your peers: players with a rating very similar to yours."),
          p(
            "It should give us some idea about what your strengths are, and where you have room for improvement."
          )
        )
      ,
      div(cls := "tutor__perfs tutor-cards")(
        full.report.perfs.toList map { perfReportCard(full.report, _, user) }
      )
    )

  private[tutor] def menu(full: TutorFullReport.Available, user: User, report: Option[TutorPerfReport])(using
      PageContext
  ) = frag(
    a(href := routes.Tutor.user(user.username), cls := report.isEmpty.option("active"))("Tutor"),
    full.report.perfs.map { p =>
      a(
        cls  := p.perf.key.value.active(report.so(_.perf.key.value)),
        href := routes.Tutor.perf(user.username, p.perf.key)
      )(p.perf.trans)
    }
  )

  private def perfReportCard(report: TutorFullReport, perfReport: TutorPerfReport, user: User)(using
      PageContext
  ) =
    st.article(
      cls      := "tutor__perfs__perf tutor-card tutor-card--link",
      dataHref := routes.Tutor.perf(user.username, perfReport.perf.key)
    )(
      div(cls := "tutor-card__top")(
        iconTag(perfReport.perf.icon),
        div(cls := "tutor-card__top__title")(
          h3(cls := "tutor-card__top__title__text")(
            perfReport.stats.totalNbGames.localize,
            " ",
            perfReport.perf.trans,
            " games"
          ),
          div(cls := "tutor-card__top__title__sub")(
            perf.timePercentAndRating(report, perfReport)
          )
        )
      ),
      div(cls := "tutor-card__content")(
        grade.peerGrade(concept.accuracy, perfReport.accuracy),
        grade.peerGrade(concept.tacticalAwareness, perfReport.awareness),
        grade.peerGrade(concept.resourcefulness, perfReport.resourcefulness),
        grade.peerGrade(concept.conversion, perfReport.conversion),
        grade.peerGrade(concept.speed, perfReport.globalClock),
        grade.peerGrade(concept.clockFlagVictory, perfReport.flagging.win),
        grade.peerGrade(concept.clockTimeUsage, perfReport.clockUsage),
        perfReport.phases.map { phase =>
          grade.peerGrade(concept.phase(phase.phase), phase.mix)
        },
        bits.seeMore
      )
    )
