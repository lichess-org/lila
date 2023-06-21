package views.html.study

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.study.Study

import controllers.routes

object create:

  private def studyButton(s: Study.IdName, chapterCount: Int) =
    val btn =
      if chapterCount >= Study.maxChapters then submitButton(cls := "disabled", st.disabled)
      else submitButton

    btn(name := "as", value := s.id, cls := "button submit")(s.name)

  def apply(
      data: lila.study.StudyForm.importGame.Data,
      owner: List[(Study.IdName, Int)],
      contrib: List[(Study.IdName, Int)],
      backUrl: Option[String]
  )(using PageContext) =
    views.html.site.message(
      title = trans.toStudy.txt(),
      icon = Some(licon.StudyBoard),
      back = backUrl,
      moreCss = cssTag("study.create").some
    ) {
      div(cls := "study-create")(
        postForm(action := routes.Study.create)(
          input(tpe := "hidden", name := "gameId", value      := data.gameId),
          input(tpe := "hidden", name := "orientation", value := data.orientation.map(_.key)),
          input(tpe := "hidden", name := "fen", value         := data.fen.map(_.value)),
          input(tpe := "hidden", name := "pgn", value         := data.pgnStr),
          input(tpe := "hidden", name := "variant", value     := data.variant.map(_.key)),
          h2(trans.study.whereDoYouWantToStudyThat()),
          p(
            submitButton(
              name     := "as",
              value    := "study",
              cls      := "submit button large new text",
              dataIcon := licon.StudyBoard
            )(trans.study.createStudy())
          ),
          div(cls := "studies")(
            div(
              h2(trans.study.myStudies()),
              owner.map(studyButton)
            ),
            div(
              h2(trans.study.studiesIContributeTo()),
              contrib.map(studyButton)
            )
          )
        )
      )
    }
