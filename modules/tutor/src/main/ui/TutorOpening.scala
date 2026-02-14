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
      as: Color,
      user: User
  ) =
    val all: List[(Color, TutorOpeningFamily)] = for
      (color, fams) <- perfReport.openings.map(_.families).zipColor.mapList(identity)
      fam <- fams
    yield (color, fam)
    lila.ui.bits.mselect(
      "tutor-report-select",
      span(s"$as ${report.family.name}"),
      all.map: (color, family) =>
        a(
          href := routes.Tutor
            .opening(user.username, perfReport.perf.key, as, family.family.key.value),
          cls := (as == color && family.family.key == report.family.key).option("current")
        )(s"$color ${family.family.name}")
    )

  def opening(
      full: TutorFullReport,
      perfReport: TutorPerfReport,
      report: TutorOpeningFamily,
      as: Color,
      user: User,
      puzzle: Option[Frag]
  )(using Context) =
    bits.page(
      title = s"Lichess Tutor • ${perfReport.perf.trans} • ${as.name} • ${report.family.name.value}",
      menu = perfUi.menu(user, perfReport, "opening".some)
    )(cls := "tutor__opening tutor-layout"):
      frag(
        div(cls := "box tutor__first-box")(
          boxTop(
            h1(
              a(
                href := bits.urlOf(user.username, perfReport.perf.key, "opening".some),
                dataIcon := Icon.LessThan,
                cls := "text"
              ),
              bits.perfSelector(full, perfReport.perf, "opening".some),
              openingSelector(perfReport, report, as, user),
              bits.otherUser(user)
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
                      .pgn(report.family.anyOpening.pgn.value.replace(" ", "_"))}#explorer/${user.username}"
                )("Personal opening explorer"),
                puzzle
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

  def openings(full: TutorFullReport, report: TutorPerfReport, user: User)(using ctx: Context) =
    bits.page(menu = perfUi.menu(user, report, "opening".some))(cls := "tutor__openings tutor-layout"):
      frag(
        div(cls := "tutor__first-box box")(
          boxTop(
            h1(
              a(
                href := routes.Tutor.perf(user.username, report.perf.key),
                dataIcon := Icon.LessThan,
                cls := "text"
              ),
              bits.perfSelector(full, report.perf, "opening".some),
              bits.reportSelector(report, "opening", user),
              bits.otherUser(user)
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
                dataHref := routes.Tutor
                  .opening(user.username, report.perf.key, color, fam.family.key.value)
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
              href := s"${routes.UserAnalysis.index}?color=${color.name}#explorer/${user.username}"
            )("Personal explorer as ", color.name)
          )
        })
      )
