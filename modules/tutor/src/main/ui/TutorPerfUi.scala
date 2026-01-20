package lila.tutor
package ui

import lila.insight.{ InsightPosition, Phase }
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import chess.Color

final class TutorPerfUi(helpers: Helpers, bits: TutorBits):
  import helpers.{ *, given }

  def apply(full: TutorFullReport, report: TutorPerfReport, user: User)(using Context) =
    bits.page(menu = menu(user, report, "perf"))(cls := "tutor__perf tutor-layout"):
      frag(
        div(cls := "box tutor__first-box")(
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
          )
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
            selectThreeOpenings(report).map: (color, fam) =>
              grade.peerGrade(concept.opening(fam.family, color), fam.mix, h4)
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
      strong(perfReport.stats.rating)
      // ". Peers rating: ",
      // strong(perfReport.stats.peers.showRatingRange)
    )
  )

  private def selectThreeOpenings(report: TutorPerfReport): List[(Color, TutorOpeningFamily)] =
    val byColor = chess.ByColor[List[TutorOpeningFamily]]: color =>
      report.openings(color).families.take(2)
    val firstTwo: List[(Color, TutorOpeningFamily)] = byColor.zipColor.flatMap: (color, fams) =>
      fams.headOption.map(color -> _)
    val extraOne: List[(Color, TutorOpeningFamily)] =
      byColor.zipColor
        .flatMap: (color, fams) =>
          fams.lift(1).map(color -> _)
        .sortBy(-_._2.performance.mine.count)
        .headOption
        .toList
    firstTwo ::: extraOne

  def menu(
      user: User,
      report: TutorPerfReport,
      active: String
  )(using Context) = frag(
    a(href := routes.Tutor.user(user.username))("Tutor"),
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
      cls := List("tutor__perf__angle tutor-card" -> true, "tutor-card--link" -> url.isDefined),
      dataHref := url
    )(
      div(cls := "tutor-card__top")(
        div(cls := "tutor-card__top__title tutor-card__top__title--pad")(
          h3(cls := "tutor-card__top__title__text")(title)
        )
      ),
      div(cls := "tutor-card__content tutor-grades")(content)
    )

  def phases(report: TutorPerfReport, user: User)(using Context) =
    bits.page(menu = menu(user, report, "phases"))(cls := "tutor__phases tutor-layout"):
      frag(
        div(cls := "tutor__first-box box")(
          frag(
            boxTop(
              h1(
                a(
                  href := routes.Tutor.perf(user.username, report.perf.key),
                  dataIcon := Icon.LessThan,
                  cls := "text"
                ),
                bits.otherUser(user),
                report.perf.trans,
                " phases"
              )
            ),
            bits.mascotSays(ul(report.phaseHighlights(3).map(compare.show)))
          )
        ),
        div(cls := "tutor-cards tutor-cards--triple")(
          report.phases.map: phase =>
            st.section(cls := "tutor-card tutor__phases__phase")(
              div(cls := "tutor-card__top")(
                div(cls := "tutor-card__top__title tutor-card__top__title--pad")(
                  concept.phaseIcon(phase.phase).frag,
                  h2(cls := "tutor-card__top__title__text")(phase.phase.name)
                )
              ),
              div(cls := "tutor-card__content tutor-grades")(
                grade
                  .peerGradeWithDetail(concept.accuracy, phase.accuracy, InsightPosition.Move)
                  .map(_(cls := "tutor-grade--narrow")),
                grade
                  .peerGradeWithDetail(concept.tacticalAwareness, phase.awareness, InsightPosition.Move)
                  .map(_(cls := "tutor-grade--narrow")),
                div(cls := "tutor__phases__phase__buttons")(
                  a(
                    cls := "button button-no-upper text",
                    dataIcon := Icon.ArcheryTarget,
                    href := routes.Puzzle.show(phase.phase.name)
                  )("Train with ", phase.phase.name, " puzzles"),
                  a(
                    cls := "button button-no-upper text",
                    dataIcon := Icon.AnalogTv,
                    href := s"${routes.Video.index}?tags=${phase.phase.name}"
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
    bits.page(menu = menu(user, report, "skills"))(cls := "tutor__skills tutor-layout"):
      frag(
        div(cls := "tutor__first-box box")(
          boxTop(
            h1(
              a(
                href := routes.Tutor.perf(user.username, report.perf.key),
                dataIcon := Icon.LessThan,
                cls := "text"
              ),
              bits.otherUser(user),
              report.perf.trans,
              " skills"
            )
          ),
          bits.mascotSays(
            ul(report.skillHighlights(3).map(compare.show))
          )
        ),
        div(cls := "tutor-grades box box-pad")(
          grade.peerGradeWithDetail(concept.accuracy, report.accuracy, InsightPosition.Move),
          grade.peerGradeWithDetail(concept.tacticalAwareness, report.awareness, InsightPosition.Move),
          grade.peerGradeWithDetail(concept.resourcefulness, report.resourcefulness, InsightPosition.Game),
          grade.peerGradeWithDetail(concept.conversion, report.conversion, InsightPosition.Game)
        )
      )

  def time(report: TutorPerfReport, user: User)(using Context) =
    bits.page(menu = menu(user, report, "time"))(cls := "tutor__time tutor-layout"):
      frag(
        div(cls := "tutor__first-box box")(
          boxTop(
            h1(
              a(
                href := routes.Tutor.perf(user.username, report.perf.key),
                dataIcon := Icon.LessThan,
                cls := "text"
              ),
              bits.otherUser(user),
              report.perf.trans,
              " time management"
            )
          ),
          bits.mascotSays(
            ul(report.timeHighlights(5).map(compare.show))
          )
        ),
        div(cls := "tutor-grades box box-pad")(
          grade.peerGradeWithDetail(concept.speed, report.globalClock, InsightPosition.Move),
          grade.peerGradeWithDetail(concept.clockFlagVictory, report.flagging.win, InsightPosition.Game),
          grade.peerGradeWithDetail(concept.clockTimeUsage, report.clockUsage, InsightPosition.Game)
        )
      )
