package lila.tutor
package ui

import lila.insight.{ InsightPosition, Phase }
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import chess.Color

final class TutorPerfUi(helpers: Helpers, bits: TutorBits):
  import helpers.{ *, given }
  import bits.urlOf

  def apply(full: TutorFullReport, report: TutorPerfReport, user: User)(using Context) =
    bits.page(menu = menu(user, report, none))(cls := "tutor__perf tutor-layout"):
      frag(
        div(cls := "box tutor__first-box")(
          boxTop(
            h1(
              a(href := routes.Tutor.user(user.username), dataIcon := Icon.LessThan, cls := "text"),
              "Tutor",
              bits.perfSelector(full, report.perf, none),
              bits.otherUser(user)
            )
          ),
          bits.mascotSays(
            p(
              "Looking at ",
              pluralizeLocalize("game", report.stats.totalNbGames),
              report.stats.dates.map: dates =>
                frag(" played between ", showDate(dates.start), " and ", showDate(dates.end), ".")
            ),
            timePercentAndRating(full, report),
            ul(TutorCompare.mixedBag(report.relevantComparisons)(4).map(compare.show(_)))
          )
        ),
        div(cls := "tutor__perf__angles tutor-cards")(
          angleCard(
            frag(report.perf.trans, " skills"),
            urlOf(user.username, report.perf.key, "skills".some).some
          )(
            grade.peerGrade(concept.accuracy, report.accuracy),
            grade.peerGrade(concept.tacticalAwareness, report.awareness),
            grade.peerGrade(concept.resourcefulness, report.resourcefulness),
            grade.peerGrade(concept.conversion, report.conversion)
          ),
          angleCard(
            frag(report.perf.trans, " openings"),
            urlOf(user.username, report.perf.key, "opening".some).some
          )(
            selectFourOpenings(report).map: (color, fam) =>
              grade.peerGrade(concept.opening(fam.family, color), fam.mix, h4)
          ),
          angleCard(
            frag(report.perf.trans, " time management"),
            (report.perf.key != PerfKey.correspondence).option:
              urlOf(user.username, report.perf.key, "time".some)
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
            urlOf(user.username, report.perf.key, "phases".some).some
          ):
            report.phases.list.map: phase =>
              grade.peerGrade(concept.phase(phase.phase), phase.mix)
          ,
          angleCard(
            frag(report.perf.trans, " pieces"),
            urlOf(user.username, report.perf.key, "pieces".some).some
          )(
            report.pieces.list.map: piece =>
              grade.peerGrade(concept.piece(piece.role), piece.mix)
          )(cls := "tutor__perf__angle--pieces")
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
      perfReport.stats.peers.value.match
        case (min, max) => s"$min to $max"
      ,
      "."
    )
  )

  private def selectFourOpenings(report: TutorPerfReport): List[(Color, TutorOpeningFamily)] =
    for
      color <- Color.all
      ops <- report.openings(color).families.take(2)
    yield color -> ops

  def menu(user: User, report: TutorPerfReport, active: Option[Angle])(using Context) = frag(
    a(href := routes.Tutor.user(user.username))("Tutor"),
    a(
      href := routes.Tutor.perf(user.username, report.perf.key),
      cls := List("active" -> active.isEmpty),
      dataIcon := report.perf.icon
    )(report.perf.trans),
    bits.reportAngles.map: (angle, name) =>
      a(
        href := urlOf(user.username, report.perf.key, angle.some),
        cls := List("active" -> active.has(angle), "subnav__subitem" -> true)
      )(name)
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

  def phases(full: TutorFullReport, report: TutorPerfReport, user: User)(using Context) =
    bits.page(menu = menu(user, report, "phases".some))(cls := "tutor__phases tutor-layout"):
      frag(
        div(cls := "tutor__first-box box")(
          frag(
            angleTop(full, report, user, "phases"),
            bits.mascotSays(ul(report.phases.highlights(3).map(compare.show(_))))
          )
        ),
        div(cls := "tutor-cards tutor-cards--triple")(
          report.phases.list.map: phase =>
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
                  a(
                    cls := "tutor-card__more",
                    href := urlOf(user.username, report.perf.key, "opening".some)
                  )(
                    "More about your ",
                    report.perf.trans,
                    " openings"
                  )
                )
              )
            )
        )
      )

  def pieces(full: TutorFullReport, report: TutorPerfReport, user: User)(using Context) =
    bits.page(menu = menu(user, report, "pieces".some))(cls := "tutor__pieces tutor-layout"):
      frag(
        div(cls := "tutor__first-box box")(
          frag(
            angleTop(full, report, user, "pieces"),
            bits.mascotSays(ul(report.pieces.highlights(3).map(compare.show(_, "with"))))
          )
        ),
        div(cls := "tutor-cards"):
          report.pieces.list.map: piece =>
            div(cls := "tutor__pieces__piece tutor-card")(
              div(cls := "tutor-card__top")(
                concept.pieceIcon(piece.role).frag,
                div(cls := "tutor-card__top__title")(
                  h3(cls := "tutor-card__top__title__text")(piece.role.name),
                  div(cls := "tutor-card__top__title__sub")(
                    bits.percentFrag(report.pieces.frequency(piece.role)),
                    " of your moves"
                  )
                )
              ),
              div(cls := "tutor-card__content tutor-grades")(
                grade.peerGrade(concept.accuracy, piece.accuracy),
                grade.peerGrade(concept.tacticalAwareness, piece.awareness)
              )
            )
      )

  def skills(full: TutorFullReport, report: TutorPerfReport, user: User)(using Context) =
    bits.page(menu = menu(user, report, "skills".some))(cls := "tutor__skills tutor-layout"):
      frag(
        div(cls := "tutor__first-box box")(
          angleTop(full, report, user, "skills"),
          bits.mascotSays(
            ul(report.skillHighlights(3).map(compare.show(_)))
          )
        ),
        div(cls := "tutor-grades box box-pad")(
          grade.peerGradeWithDetail(concept.accuracy, report.accuracy, InsightPosition.Move),
          grade.peerGradeWithDetail(concept.tacticalAwareness, report.awareness, InsightPosition.Move),
          grade.peerGradeWithDetail(concept.resourcefulness, report.resourcefulness, InsightPosition.Game),
          grade.peerGradeWithDetail(concept.conversion, report.conversion, InsightPosition.Game)
        )
      )

  def time(full: TutorFullReport, report: TutorPerfReport, user: User)(using Context) =
    bits.page(menu = menu(user, report, "time".some))(cls := "tutor__time tutor-layout"):
      frag(
        div(cls := "tutor__first-box box")(
          angleTop(full, report, user, "time"),
          bits.mascotSays(
            ul(report.timeHighlights(5).map(compare.show(_)))
          )
        ),
        div(cls := "tutor-grades box box-pad")(
          grade.peerGradeWithDetail(concept.speed, report.globalClock, InsightPosition.Move),
          grade.peerGradeWithDetail(concept.clockFlagVictory, report.flagging.win, InsightPosition.Game),
          grade.peerGradeWithDetail(concept.clockTimeUsage, report.clockUsage, InsightPosition.Game)
        )
      )

  private def angleTop(full: TutorFullReport, report: TutorPerfReport, user: User, angle: Angle)(using
      Context
  ) =
    boxTop:
      h1(
        backToPerf(report, user),
        bits.perfSelector(full, report.perf, angle.some),
        bits.reportSelector(report, angle, user),
        bits.otherUser(user)
      )

  private def backToPerf(report: TutorPerfReport, user: User) =
    a(
      href := routes.Tutor.perf(user.username, report.perf.key),
      dataIcon := Icon.LessThan,
      cls := "text"
    )
