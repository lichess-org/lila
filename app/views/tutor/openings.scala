package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{
  Rating,
  TutorBothValueOptions,
  TutorBothValues,
  TutorFullReport,
  TutorPerfReport,
  ValueCount
}

object openings {

  def apply(full: TutorFullReport.Available, report: TutorPerfReport, user: lila.user.User)(implicit
      ctx: Context
  ) =
    bits.layout(full, menu = perf.menu(full, user, report, "openings"))(
      cls := "tutor__openings box",
      h1(
        a(href := routes.Tutor.perf(user.username, report.perf.key), dataIcon := "", cls := "text"),
        report.perf.trans,
        " openings"
      ),
      bits.mascotSays(report openingHighlights 3 map compare.show),
      div(cls := "tutor__openings__colors tutor__pad")(chess.Color.all.map { color =>
        st.section(cls := "tutor__openings__color")(
          h2("Your most played ", color.name, " openings"),
          div(cls := "tutor__openings__color__openings")(report.openings(color).families.map { fam =>
            div(
              cls := "tutor__openings__opening tutor-card tutor-card--link",
              dataHref := routes.Tutor
                .opening(user.username, report.perf.key, color.name, fam.family.key.value)
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
              div(cls := "tutor-card__content")(
                grade.peerGrade(concept.performance, fam.performance.toOption),
                grade.peerGrade(concept.accuracy, fam.accuracy),
                grade.peerGrade(concept.tacticalAwareness, fam.awareness)
              )
            )
          })
        )
      })
    )

  private val pieceTag = tag("piece")
}
