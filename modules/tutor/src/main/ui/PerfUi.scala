package lila.tutor
package ui

import chess.format.pgn.PgnStr

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.perf.UserWithPerfs
import lila.insight.{ Phase, InsightPosition }

final class PerfUi(helpers: Helpers, bits: TutorBits):
  import helpers.{ *, given }

  def apply(full: TutorFullReport, report: TutorPerfReport, user: User)(using Context) =
    frag(
      boxTop(
        h1(
          a(href := routes.Tutor.user(user.username), dataIcon := Icon.LessThan, cls := "text"),
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
        ul(TutorCompare.mixedBag(report.relevantComparisons)(4).map(compare.show))
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
            report.openings(color).families.headOption.map { fam =>
              grade.peerGrade(concept.adhoc(s"${fam.family.name} as $color"), fam.mix, h4)
            }
        ),
        angleCard(
          frag(report.perf.trans, " time management"),
          (report.perf.key != PerfKey.correspondence)
            .option(routes.Tutor.time(user.username, report.perf.key))
        )(
          if report.perf.key == PerfKey.correspondence then p("Not applicable.")
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
    report.percentTimeOf(perfReport.perf).map { percent =>
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

  def menu(
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

  def phases(report: TutorPerfReport, user: User)(using Context) =
    frag(
      boxTop(
        h1(
          a(
            href     := routes.Tutor.perf(user.username, report.perf.key),
            dataIcon := Icon.LessThan,
            cls      := "text"
          ),
          bits.otherUser(user),
          report.perf.trans,
          " phases"
        )
      ),
      bits.mascotSays(ul(report.phaseHighlights(3).map(compare.show))),
      div(cls := "tutor-cards tutor-cards--triple")(
        report.phases.map: phase =>
          st.section(cls := "tutor-card tutor__phases__phase")(
            div(cls := "tutor-card__top")(
              div(cls := "tutor-card__top__title tutor-card__top__title--pad")(
                h2(cls := "tutor-card__top__title__text")(phase.phase.name)
              )
            ),
            div(cls := "tutor-card__content")(
              grade.peerGradeWithDetail(concept.accuracy, phase.accuracy, InsightPosition.Move),
              grade.peerGradeWithDetail(concept.tacticalAwareness, phase.awareness, InsightPosition.Move),
              div(cls := "tutor__phases__phase__buttons")(
                a(
                  cls      := "button button-no-upper text",
                  dataIcon := Icon.ArcheryTarget,
                  href     := routes.Puzzle.show(phase.phase.name)
                )("Train with ", phase.phase.name, " puzzles"),
                a(
                  cls      := "button button-no-upper text",
                  dataIcon := Icon.AnalogTv,
                  href     := s"${routes.Video.index}?tags=${phase.phase.name}"
                )("Watch ", phase.phase.name, " videos")
              ),
              (phase.phase == Phase.Opening).option(
                a(cls := "tutor-card__more", href := routes.Tutor.openings(user.username, report.perf.key))(
                  "More about your ",
                  report.perf.trans,
                  " openings"
                )
              )
            )
          )
      )
    )

  def skills(report: TutorPerfReport, user: User)(using Context) =
    frag(
      boxTop(
        h1(
          a(
            href     := routes.Tutor.perf(user.username, report.perf.key),
            dataIcon := Icon.LessThan,
            cls      := "text"
          ),
          bits.otherUser(user),
          report.perf.trans,
          " skills"
        )
      ),
      bits.mascotSays(
        ul(report.skillHighlights(3).map(compare.show))
      ),
      div(cls := "tutor__pad")(
        grade.peerGradeWithDetail(concept.accuracy, report.accuracy, InsightPosition.Move),
        hr,
        grade.peerGradeWithDetail(concept.tacticalAwareness, report.awareness, InsightPosition.Move),
        hr,
        grade.peerGradeWithDetail(concept.resourcefulness, report.resourcefulness, InsightPosition.Game),
        hr,
        grade.peerGradeWithDetail(concept.conversion, report.conversion, InsightPosition.Game)
      )
    )

  def time(report: TutorPerfReport, user: User)(using Context) =
    frag(
      boxTop(
        h1(
          a(
            href     := routes.Tutor.perf(user.username, report.perf.key),
            dataIcon := Icon.LessThan,
            cls      := "text"
          ),
          bits.otherUser(user),
          report.perf.trans,
          " time management"
        )
      ),
      bits.mascotSays(
        ul(report.timeHighlights(5).map(compare.show))
      ),
      div(cls := "tutor__pad")(
        grade.peerGradeWithDetail(concept.speed, report.globalClock, InsightPosition.Move),
        hr,
        grade.peerGradeWithDetail(concept.clockFlagVictory, report.flagging.win, InsightPosition.Game),
        hr,
        grade.peerGradeWithDetail(concept.clockTimeUsage, report.clockUsage, InsightPosition.Game)
      )
    )
