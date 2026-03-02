package lila.tutor
package ui

import lila.insight.InsightPosition
import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class TutorOpening(helpers: Helpers, bits: TutorBits, perfUi: TutorPerfUi):
  import helpers.{ *, given }

  private def openingSelector(
      perfReport: TutorPerfReport,
      report: TutorOpeningFamily,
      as: Color
  )(using config: TutorConfig) =
    val all: List[(Color, TutorOpeningFamily)] = for
      (color, fams) <- perfReport.openings.map(_.families).zipColor.mapList(identity)
      fam <- fams
    yield (color, fam)
    lila.ui.bits.mselect(
      "tutor-report-select",
      span(s"$as ${report.family.name}"),
      all.map: (color, family) =>
        a(
          href := config.url.opening(perfReport.perf, color, family.family),
          cls := (as == color && family.family.key == report.family.key).option("current")
        )(s"$color ${family.family.name}")
    )

  def opening(
      full: TutorFullReport,
      perfReport: TutorPerfReport,
      report: TutorOpeningFamily,
      as: Color
  )(using Context) =
    bits.page(
      title = s"Lichess Tutor • ${perfReport.perf.trans} • ${as.name} • ${report.family.name.value}",
      menu = perfUi.menu(perfReport, "opening".some)(using full.config)
    )(cls := "tutor__opening tutor-layout"):
      frag(
        div(cls := "box")(
          boxTop(
            h1(
              a(href := full.url.angle(perfReport.perf, "opening"), dataIcon := Icon.LessThan),
              bits.perfSelector(full, perfReport.perf, "opening".some),
              openingSelector(perfReport, report, as)(using full.config),
              bits.otherUser(full.user)
            )
          ),
          bits.mascotSays(
            div(
              cls := "lpv lpv--todo",
              st.data("pgn") := report.family.anyOpening.pgn,
              st.data("title") := report.family.name,
              st.data("orientation") := as.name
            ),
            div(cls := "mascot-says__content__text")(
              p(
                "You played the ",
                report.family.name.value,
                " in ",
                report.performance.mine.count.localize,
                " games, which is ",
                bits.percentFrag(perfReport.openingFrequency(as, report)),
                " of the time you played as ",
                as.name,
                "."
              ),
              div(cls := "mascot-says__buttons")(
                a(
                  cls := "button button-no-upper text",
                  dataIcon := Icon.InfoCircle,
                  href := bits.openingUrl(report.family.anyOpening)
                )("Learn about this opening"),
                a(
                  cls := "button button-no-upper text",
                  dataIcon := Icon.Book,
                  href := s"${routes.UserAnalysis
                      .pgn(report.family.anyOpening.pgn.value.replace(" ", "_"))}#explorer/${full.user}"
                )("Personal opening explorer"),
                a(
                  cls := "button button-no-upper text",
                  dataIcon := Icon.ArcheryTarget,
                  href := routes.Puzzle.angleAndColor(report.family.key.value, as.name)
                )("Train with puzzles")
              )
            )
          )
        ),
        div(cls := "box box-pad tutor-grades")(
          grade.peerGradeWithDetail(concept.performance, report.performance.some, InsightPosition.Game),
          grade.peerGradeWithDetail(concept.accuracy, report.accuracy, InsightPosition.Move),
          grade.peerGradeWithDetail(concept.tacticalAwareness, report.awareness, InsightPosition.Move)
        )
      )

  def openings(full: TutorFullReport, report: TutorPerfReport)(using ctx: Context) =
    given TutorConfig = full.config
    bits.page(menu = perfUi.menu(report, "opening".some))(
      cls := "tutor__openings tutor-layout"
    ):
      frag(
        div(cls := "box")(
          boxTop(
            h1(
              a(href := full.config.url.perf(report.perf), dataIcon := Icon.LessThan),
              bits.perfSelector(full, report.perf, "opening".some),
              bits.reportSelector(report, "opening"),
              bits.otherUser(full.user)
            )
          ),
          bits.mascotSays(
            report.openingHighlights(3).map(compare.show(_))
          )
        ),
        div(cls := "tutor__openings__colors tutor__pad")(Color.all.map { color =>
          st.section(cls := "tutor__openings__color")(
            h2("Your ", color.name, " openings"),
            div(cls := "tutor__openings__color__openings")(report.openings(color).families.map { fam =>
              val opening = fam.family.anyOpening
              div(
                cls := "tutor__openings__opening tutor-card tutor-card--link",
                dataHref := full.url.opening(report.perf, color, fam.family)
              )(
                div(cls := "tutor-card__top")(
                  div(cls := "tutor-card__top__board")(
                    chessgroundMini(opening.fen.board, color, lastMove = opening.lastUci)(div)
                  ),
                  div(cls := "tutor-card__top__title")(
                    h3(cls := "tutor-card__top__title__text")(fam.family.name.value),
                    div(cls := "tutor-card__top__title__sub")(
                      bits.percentFrag(report.openingFrequency(color, fam)),
                      " of your games"
                    )
                  )
                ),
                div(cls := "tutor-card__content tutor-grades")(
                  grade.peerGrade(concept.performance, fam.performance.some),
                  grade.peerGrade(concept.accuracy, fam.accuracy),
                  grade.peerGrade(concept.tacticalAwareness, fam.awareness)
                )
              )
            }),
            a(
              cls := "tutor__openings__color__explorer button button-no-upper text",
              dataIcon := Icon.Book,
              href := s"${routes.UserAnalysis.index}?color=${color.name}#explorer/${full.user}"
            )("Personal explorer as ", color.name)
          )
        })
      )
