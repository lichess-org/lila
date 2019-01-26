package views.html.study

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object clone {

  def apply(s: lila.study.Study)(implicit ctx: Context) =
    views.html.site.message(
      title = s"Clone ${s.name}",
      icon = Some("4"),
      back = false
    ) {
        form(action := routes.Study.cloneApply(s.id.value), method := "POST")(
          p("This will create a new private study with the same chapters."),
          p("You will be the owner of that new study."),
          p("The two studies can be updated separately."),
          p("Deleting one study will <strong>not</strong> delete the other study."),
          p(
            button(`type` := "submit", cls := "submit button large text", dataIcon := "4",
              style := "margin: 30px auto; display: block; font-size: 2em;")("Clone the study")
          ),
          p(
            a(href := routes.Study.show(s.id.value), cls := "text", dataIcon := "I")(trans.cancel())
          )
        )
      }
}
