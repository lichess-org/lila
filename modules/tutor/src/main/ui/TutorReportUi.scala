package lila.tutor
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class TutorReportUi(helpers: Helpers, bits: TutorBits, perfUi: TutorPerfUi):
  import helpers.{ *, given }

  def apply(full: TutorFullReport)(using Context) =
    bits.page(menu = bits.menu(full, none))(cls := "tutor__report tutor-layout"):
      frag(
        div(cls := "box")(
          boxTop(
            h1(
              a(href := routes.Tutor.user(full.user), dataIcon := Icon.LessThan),
              "Lichess Tutor",
              bits.beta,
              bits.otherUser(full.user)
            )
          ),
          bits.mascotSays(
            div(cls := "tutor__report__header")(
              bits.reportTime(full.config),
              bits.reportMeta(full.nbGames, full.stats.meanRating),
              postForm(
                cls := "tutor__report__delete",
                action := routes.Tutor.delete(full.user.id, full.config.rangeStr)
              ):
                button(tpe := "submit")(trans.site.delete)
            ),
            if full.perfs.isEmpty then p("Not enough rated games to examine!")
            else
              p(
                "Each aspect of your playstyle is compared to other players of similar rating, called \"peers\".",
                br,
                "It should give you some idea about what your strengths are, and where you have room for improvement."
              )
          )
        ),
        full.perfs.nonEmpty.option(tutorConcepts),
        div(cls := "tutor__perfs tutor-cards")(
          full.perfs.toList.map { perfReportCard(full, _) }
        )
      )

  private def perfReportCard(report: TutorFullReport, perfReport: TutorPerfReport)(using
      Context
  ) =
    st.article(
      cls := "tutor__perfs__perf tutor-card tutor-card--link",
      dataHref := report.url.perf(perfReport.perf)
    )(
      div(cls := "tutor-card--perf__top")(
        iconTag(perfReport.perf.icon),
        div(cls := "tutor-card--perf__top__title")(
          h3(cls := "tutor-card--perf__top__title__text")(
            perfReport.stats.totalNbGames.localize,
            " ",
            perfReport.perf.trans,
            " games"
          ),
          div(cls := "tutor-card--perf__top__title__sub")(
            perfUi.timePercentAndRating(report, perfReport)
          )
        )
      ),
      div(cls := "tutor-card__content tutor-grades")(
        grade.peerGrade(concept.accuracy, perfReport.accuracy),
        grade.peerGrade(concept.tacticalAwareness, perfReport.awareness),
        grade.peerGrade(concept.resourcefulness, perfReport.resourcefulness),
        grade.peerGrade(concept.conversion, perfReport.conversion),
        grade.peerGrade(concept.speed, perfReport.globalClock),
        grade.peerGrade(concept.clockFlagVictory, perfReport.flagging.win),
        grade.peerGrade(concept.clockTimeUsage, perfReport.clockUsage),
        perfReport.phases.list.map: phase =>
          grade.peerGrade(concept.phase(phase.phase), phase.mix),
        bits.seeMore
      )
    )

  private def tutorConcept(icon: Frag, name: Frag, desc: Frag) =
    div(cls := "tutor-concept")(
      div(cls := "tutor-concept__icon")(icon),
      div(cls := "tutor-concept__content")(
        h3(cls := "tutor-concept__name")(name),
        div(cls := "tutor-concept__desc")(desc)
      )
    )

  private def tutorConcepts =
    fieldset(cls := "tutor__concepts toggle-box toggle-box--toggle toggle-box--toggle-off")(
      legend("Tutor concepts"),
      div(cls := "tutor-concepts")(
        tutorConcept(
          iconTag(Icon.Group),
          "Peers",
          frag(
            strong("Players with a rating similar to yours, in a given time control."),
            p(
              "Each aspect of your playstyle is compared to that of your peers, giving you a concrete idea of how you perform in each area compared to players of similar strength."
            )
          )
        ),
        List(
          concept.accuracy,
          concept.tacticalAwareness,
          concept.resourcefulness,
          concept.conversion,
          concept.performance,
          concept.speed,
          concept.clockFlagVictory,
          concept.clockTimeUsage
        ).map: c =>
          tutorConcept(c.icon.frag, concept.show(c), frag(strong(c.descShort), c.descLong.map(p(_))))
      )
    )
