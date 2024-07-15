package lila.study
package ui

import lila.core.study.IdName
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StudyUi(helpers: Helpers):
  import helpers.{ *, given }

  def clone(s: Study)(using Context) =
    postForm(action := routes.Study.cloneApply(s.id))(
      p("This will create a new private study with the same chapters."),
      p("You will be the owner of that new study."),
      p("The two studies can be updated separately."),
      p("Deleting one study will ", strong("not"), " delete the other study."),
      p(
        submitButton(
          cls      := "submit button large text",
          dataIcon := Icon.StudyBoard,
          style    := "margin: 30px auto; display: block; font-size: 2em;"
        )("Clone the study")
      ),
      p(
        a(href := routes.Study.show(s.id), cls := "text", dataIcon := Icon.LessThan)(trans.site.cancel())
      )
    )

  private def studyButton(s: IdName, chapterCount: Int) =
    val btn =
      if Study.maxChapters <= chapterCount then submitButton(cls := "disabled", st.disabled)
      else submitButton

    btn(name := "as", value := s.id, cls := "button submit")(s.name)

  def create(
      data: lila.study.StudyForm.importGame.Data,
      owner: List[(IdName, Int)],
      contrib: List[(IdName, Int)],
      backUrl: Option[String]
  )(using Context) =
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
            dataIcon := Icon.StudyBoard
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
