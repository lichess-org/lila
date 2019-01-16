package views.html.study

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.study.Study

import controllers.routes

object create {

  private def studyButton(s: Study.IdName) =
    button(name := "as", value := s.id.value, `type` := "submit", cls := "submit button")(s.name.value)

  def apply(data: lila.study.DataForm.importGame.Data, owner: List[Study.IdName], contrib: List[Study.IdName])(implicit ctx: Context) =
    views.html.site.message(
      title = "Study",
      icon = Some("4"),
      back = true,
      moreCss = cssTag("study-create.css").some
    ) {
        form(action := routes.Study.create, method := "POST")(
          input(`type` := "hidden", name := "gameId", value := data.gameId),
          input(`type` := "hidden", name := "orientation", value := data.orientationStr),
          input(`type` := "hidden", name := "fen", value := data.fenStr),
          input(`type` := "hidden", name := "pgn", value := data.pgnStr),
          input(`type` := "hidden", name := "variant", value := data.variantStr),
          h2("So, where do you want to study that?"),
          p(
            button(name := "as", value := "study",
              `type` := "submit", cls := "submit button large new text", dataIcon := "4")("New study")
          ),
          div(cls := "studies")(
            div(
              h2("My studies"),
              owner map studyButton
            ),
            div(
              h2("Studies I contribute to"),
              contrib map studyButton
            )
          )
        )
      }
}
