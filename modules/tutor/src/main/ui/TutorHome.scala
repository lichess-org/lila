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
          boxTop(h1("Lichess Tutor", bits.beta, bits.otherUser(user))),
          if full.report.perfs.isEmpty then empty.mascotSaysInsufficient
          else
            bits.mascotSays(
              p(
                strong(
                  cls := "tutor__intro",
                  "Analysis complete on ",
                  full.report.nbGames.localize,
                  " recent rated games of yours."
                )
              ),
              p(
                "Each aspect of your playstyle is compared to other players of similar rating, called \"peers\"."
              ),
              p(
                "It should give you some idea about what your strengths are, and where you have room for improvement."
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

    private def whatTutorIsAbout = frag(
      h2("What are your strengths and weaknesses?"),
      p("Lichess can examine your games and compare your playstyle to other players with similar rating."),
      br,
      p(
        "Tutor is all about statistical analysis and comparison to peers.",
        br,
        "No AI nonsense and no gimmicks; just concrete data about key metrics of your playstyle."
      )
    )

    private def nbGames(user: UserWithPerfs)(using Translate): String =
      lila.rating.PerfType.standardWithUltra
        .foldLeft(0)((nb, pt) => nb + user.perfs(pt).nb)
        .atMost(10_000)
        .localize

    private def examinationMethod = ol(
      li("Analyse many of your games with ", lila.ui.bits.engineFullName),
      li("Build detailed insight reports for each of your games"),
      li("Compare these insights to other players with the same rating")
    )

    def start(user: User)(using Context) =
      bits.page(menu = emptyFrag, pageSmall = true)(cls := "tutor__empty box"):
        frag(
          boxTop(h1("Lichess Tutor", bits.beta, bits.otherUser(user))),
          bits.mascotSays(
            whatTutorIsAbout
          ),
          postForm(cls := "tutor__empty__cta", action := routes.Tutor.refresh(user.username)):
            submitButton(cls := "button button-fat button-no-upper")("Compute my tutor report")
        )

    def queued(in: TutorQueue.InQueue, user: UserWithPerfs, waitGames: List[(Pov, PgnStr)])(using Context) =
      bits.page(menu = emptyFrag, title = "Lichess Tutor - Examining games...", pageSmall = true)(
        cls := "tutor__empty tutor__queued box"
      ):
        frag(
          boxTop(h1("Lichess Tutor", bits.beta, bits.otherUser(user))),
          bits.mascotSays(
            whatTutorIsAbout,
            br,
            p(
              strong(cls := "tutor__intro")("You have ", nbGames(user), " games to look at. Here's the plan:")
            ),
            examinationMethod,
            p(
              (in.position > 10).option:
                frag("There are ", (in.position - 1), " players in the queue before you.", br)
              ,
              "Your report should be ready in about ",
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
