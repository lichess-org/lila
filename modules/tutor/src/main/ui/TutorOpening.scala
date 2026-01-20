package lila.tutor
package ui

import lila.insight.InsightPosition
import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class TutorOpening(helpers: Helpers, bits: TutorBits, perfUi: TutorPerfUi):
  import helpers.{ *, given }

  def openingMenu(perfReport: TutorPerfReport, report: TutorOpeningFamily, as: Color, user: User) =
    frag(
      perfReport.openings(as).families.map { family =>
        a(
          href := routes.Tutor
            .opening(user.username, perfReport.perf.key, as, family.family.key.value),
          cls := family.family.key.value.active(report.family.key.value)
        )(family.family.name.value)
      }
    )

  def opening(
      perfReport: TutorPerfReport,
      report: TutorOpeningFamily,
      as: Color,
      user: User,
      puzzle: Option[Frag]
  )(using Context) =
    bits.page(
      title = s"Lichess Tutor • ${perfReport.perf.trans} • ${as.name} • ${report.family.name.value}",
      menu = openingMenu(perfReport, report, as, user)
    )(cls := "tutor__opening tutor-layout"):
      frag(
        div(cls := "box tutor__first-box")(
          boxTop(
            h1(
              a(
                href := routes.Tutor.openings(user.username, perfReport.perf.key),
                dataIcon := Icon.LessThan,
                cls := "text"
              ),
              bits.otherUser(user),
              perfReport.perf.trans,
              ": ",
              report.family.name,
              " as ",
              as.name
            )
          ),
          bits.mascotSays(
            div(
              cls := "lpv lpv--todo",
              st.data("pgn") := report.family.anyOpening.pgn,
              st.data("title") := report.family.name
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
          grade.peerGradeWithDetail(concept.performance, report.performance.toOption, InsightPosition.Game),
          grade.peerGradeWithDetail(concept.accuracy, report.accuracy, InsightPosition.Move),
          grade.peerGradeWithDetail(concept.tacticalAwareness, report.awareness, InsightPosition.Move)
        )
      )

  def openings(report: TutorPerfReport, user: User)(using ctx: Context) =
    bits.page(menu = perfUi.menu(user, report, "openings"))(cls := "tutor__openings tutor-layout"):
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
              " openings"
            )
          ),
          bits.mascotSays(
            report.openingHighlights(3).map(compare.show)
          )
        ),
        div(cls := "tutor__openings__colors tutor__pad")(Color.all.map { color =>
          st.section(cls := "tutor__openings__color")(
            h2("Your ", color.name, " openings"),
            div(cls := "tutor__openings__color__openings")(report.openings(color).families.map { fam =>
              div(
                cls := "tutor__openings__opening tutor-card tutor-card--link",
                dataHref := routes.Tutor
                  .opening(user.username, report.perf.key, color, fam.family.key.value)
              )(
                div(cls := "tutor-card__top")(
                  div(cls := "no-square")(pieceTag(cls := s"pawn ${color.name}")),
                  div(cls := "tutor-card__top__title")(
                    h3(cls := "tutor-card__top__title__text")(fam.family.name.value),
                    div(cls := "tutor-card__top__title__sub")(
                      bits.percentFrag(report.openingFrequency(color, fam)),
                      " of your games"
                    )
                  )
                ),
                div(cls := "tutor-card__content tutor-grades")(
                  grade.peerGrade(concept.performance, fam.performance.toOption),
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

  private val pieceTag = tag("piece")
