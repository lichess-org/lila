package views.html.study

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
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
      moreCss = cssTag("study.create").some
    ) {
        div(cls := "study-create")(
          form(action := routes.Study.create, method := "POST")(
            input(tpe := "hidden", name := "gameId", value := data.gameId),
            input(tpe := "hidden", name := "orientation", value := data.orientationStr),
            input(tpe := "hidden", name := "fen", value := data.fenStr),
            input(tpe := "hidden", name := "pgn", value := data.pgnStr),
            input(tpe := "hidden", name := "variant", value := data.variantStr),
            h2("So, where do you want to study that?"),
            p(
              button(name := "as", value := "study",
                tpe := "submit", cls := "submit button large new text", dataIcon := "4")("New study")
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
        )
      }
}
