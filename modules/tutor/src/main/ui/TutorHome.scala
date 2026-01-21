package lila.tutor
package ui

import chess.format.pgn.PgnStr

import lila.core.perf.UserWithPerfs
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class TutorHome(helpers: Helpers, bits: TutorBits, perfUi: TutorPerfUi):
  import helpers.{ *, given }

  def apply(full: TutorFullReport.Available, user: User)(using Context) =
    bits.page(menu = bits.menu(full, user, none))(cls := "tutor__home tutor-layout"):
      frag(
        div(cls := "box tutor__first-box")(
          boxTop(h1("Lichess Tutor", strong(cls := "tutor__beta")("BETA"), bits.otherUser(user))),
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
        ),
        div(cls := "tutor__perfs tutor-cards")(
          full.report.perfs.toList.map { perfReportCard(full.report, _, user) }
        )
      )

  private def waitGame(game: (Pov, PgnStr)) =
    div(
      cls := "tutor__waiting-game lpv lpv--todo lpv--moves-false lpv--controls-false",
      st.data("pgn") := game._2.value,
      st.data("pov") := game._1.color.name
    )

  private def nbGames(user: UserWithPerfs)(using Translate) =
    val nb = lila.rating.PerfType.standardWithUltra.foldLeft(0): (nb, pt) =>
      nb + user.perfs(pt).nb
    p(s"Looks like you have ", strong(nb.atMost(10_000).localize), " rated games to look at, excellent!")

  private def examinationMethod = p(
    "Using the best chess engine: ",
    lila.ui.bits.engineFullName,
    ", ",
    "and comparing your playstyle to thousands of other players with similar rating."
  )

  private def perfReportCard(report: TutorFullReport, perfReport: TutorPerfReport, user: User)(using
      Context
  ) =
    st.article(
      cls := "tutor__perfs__perf tutor-card tutor-card--link",
      dataHref := routes.Tutor.perf(user.username, perfReport.perf.key)
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
        perfReport.phases.map: phase =>
          grade.peerGrade(concept.phase(phase.phase), phase.mix),
        bits.seeMore
      )
    )

  object empty:

    def start(user: User)(using Context) =
      bits.page(menu = emptyFrag, pageSmall = true)(cls := "tutor__empty box"):
        frag(
          boxTop(h1(bits.otherUser(user), "Lichess Tutor")),
          bits.mascotSays("Explain what tutor is about here."),
          postForm(cls := "tutor__empty__cta", action := routes.Tutor.refresh(user.username))(
            submitButton(cls := "button button-fat button-no-upper")("Analyse my games and help me improve")
          )
        )

    def queued(in: TutorQueue.InQueue, user: UserWithPerfs, waitGames: List[(Pov, PgnStr)])(using
        Context
    ) =
      bits.page(menu = emptyFrag, title = "Lichess Tutor - Examining games...", pageSmall = true)(
        cls := "tutor__empty tutor__queued box"
      ):
        frag(
          boxTop(h1(bits.otherUser(user), "Lichess Tutor")),
          bits.mascotSays(
            p(strong(cls := "tutor__intro")("I'm examining your games.")),
            examinationMethod,
            nbGames(user),
            p(
              "There are ",
              (in.position - 1),
              " players in the queue before you.",
              br,
              "You will get your results in about ",
              showMinutes(in.eta.toMinutes.toInt.atLeast(1)),
              "."
            )
          ),
          div(cls := "tutor__waiting-games"):
            div(cls := "tutor__waiting-games__carousel")(waitGames.map(waitGame))
        )

    def insufficientGames(user: User)(using Context) =
      bits.page(menu = emptyFrag, pageSmall = true)(cls := "tutor__insufficient box"):
        frag(
          boxTop(h1(bits.otherUser(user), "Lichess Tutor")),
          mascotSaysInsufficient
        )

    def mascotSaysInsufficient =
      bits.mascotSays(
        frag(
          strong("Not enough rated games to examine!"),
          br,
          "Please come back after you have played more chess."
        )
      )
