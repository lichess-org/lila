package views.html.study

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.study.Study
import lila.core.study.IdName

object create:

  private def studyButton(s: IdName, chapterCount: Int) =
    val btn =
      if Study.maxChapters <= chapterCount then submitButton(cls := "disabled", st.disabled)
      else submitButton

    btn(name := "as", value := s.id, cls := "button submit")(s.name)

  def apply(
      data: lila.study.StudyForm.importGame.Data,
      owner: List[(IdName, Int)],
      contrib: List[(IdName, Int)],
      backUrl: Option[String]
  )(using PageContext) =
    views.html.site.message(
      title = trans.site.toStudy.txt(),
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
