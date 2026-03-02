package lila.tutor
package ui

import lila.insight.{ InsightPosition, Phase }
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import chess.Color

final class TutorPerfUi(helpers: Helpers, bits: TutorBits):
  import helpers.given

  def apply(full: TutorFullReport, report: TutorPerfReport)(using Context) =
    given TutorConfig = full.config
    bits.page(menu = menu(report, none))(cls := "tutor__perf tutor-layout"):
      frag(
        div(cls := "box")(
          boxTop(
            h1(
              a(href := full.url.root, dataIcon := Icon.LessThan),
              "Tutor",
              bits.perfSelector(full, report.perf, none),
              bits.otherUser(full.user)
            )
          ),
          bits.mascotSays(
            div(cls := "tutor__report__header")(
              bits.reportTime(full.config),
              bits.reportMeta(report.stats.totalNbGames, report.stats.rating.some)
            ),
            timePercentAndRating(full, report),
            ul(TutorCompare.mixedBag(report.relevantComparisons)(4).map(compare.show(_)))
          )
        ),
        div(cls := "tutor__perf__angles tutor-cards")(
          angleCard(
            frag(report.perf.trans, " skills"),
            full.url.angle(report.perf, "skills").some
          )(
            grade.peerGrade(concept.accuracy, report.accuracy),
            grade.peerGrade(concept.tacticalAwareness, report.awareness),
            grade.peerGrade(concept.resourcefulness, report.resourcefulness),
            grade.peerGrade(concept.conversion, report.conversion)
          ),
          angleCard(
            frag(report.perf.trans, " openings"),
            full.url.angle(report.perf, "opening").some
          )(
            cls := (if report.variant.exotic then "tutor__perf__angle--na" else ""),
            if report.variant.exotic
            then "Not applicable to variants"
            else
              selectFourOpenings(report).map: (color, fam) =>
                grade.peerGrade(concept.opening(fam.family, color), fam.mix, h4)
          ),
          angleCard(
            frag(report.perf.trans, " time management"),
            (report.perf.key != PerfKey.correspondence).option:
              full.url.angle(report.perf, "time")
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
            full.url.angle(report.perf, "phases").some
          ):
            report.phases.list.map: phase =>
              grade.peerGrade(concept.phase(phase.phase), phase.mix)
          ,
          angleCard(
            frag(report.perf.trans, " pieces"),
            full.url.angle(report.perf, "pieces").some
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

  def menu(report: TutorPerfReport, active: Option[Angle])(using config: TutorConfig)(using Context) = frag(
    bits.menuBase(report.some),
    a(
      href := config.url.perf(report.perf),
      cls := List("active" -> active.isEmpty),
      dataIcon := report.perf.icon
    )(report.perf.trans),
    bits.reportAngles.map: (angle, name) =>
      a(
        href := config.url.angle(report.perf, angle),
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

  def phases(full: TutorFullReport, report: TutorPerfReport)(using Context) =
    given TutorConfig = full.config
    bits.page(menu = menu(report, "phases".some))(cls := "tutor__phases tutor-layout"):
      frag(
        div(cls := "box")(
          frag(
            angleTop(full, report, "phases"),
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
                    href := full.url.angle(report.perf, "opening")
                  )("More about your ", report.perf.trans, " openings")
                )
              )
            )
        )
      )

  def pieces(full: TutorFullReport, report: TutorPerfReport)(using Context) =
    given TutorConfig = full.config
    bits.page(menu = menu(report, "pieces".some))(cls := "tutor__pieces tutor-layout"):
      frag(
        div(cls := "box")(
          frag(
            angleTop(full, report, "pieces"),
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

  def skills(full: TutorFullReport, report: TutorPerfReport)(using Context) =
    given TutorConfig = full.config
    bits.page(menu = menu(report, "skills".some))(cls := "tutor__skills tutor-layout"):
      frag(
        div(cls := "box")(
          angleTop(full, report, "skills"),
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

  def time(full: TutorFullReport, report: TutorPerfReport)(using Context) =
    given TutorConfig = full.config
    bits.page(menu = menu(report, "time".some))(cls := "tutor__time tutor-layout"):
      frag(
        div(cls := "box")(
          angleTop(full, report, "time"),
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

  private def angleTop(full: TutorFullReport, report: TutorPerfReport, angle: Angle)(using
      Context,
      TutorConfig
  ) =
    boxTop:
      h1(
        a(href := full.url.perf(report.perf), dataIcon := Icon.LessThan),
        bits.perfSelector(full, report.perf, angle.some),
        bits.reportSelector(report, angle),
        bits.otherUser(full.user)
      )
