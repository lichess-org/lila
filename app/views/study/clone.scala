package views.html.study

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object clone:

  def apply(s: lila.study.Study)(implicit ctx: Context) =
    views.html.site.message(
      title = s"Clone ${s.name}",
      icon = Some("")
    ) {
      postForm(action := routes.Study.cloneApply(s.id))(
        p("This will create a new private study with the same chapters."),
        p("You will be the owner of that new study."),
        p("The two studies can be updated separately."),
        p("Deleting one study will ", strong("not"), " delete the other study."),
        p(
          submitButton(
            cls      := "submit button large text",
            dataIcon := "",
            style    := "margin: 30px auto; display: block; font-size: 2em;"
          )("Clone the study")
        ),
        p(
          a(href := routes.Study.show(s.id), cls := "text", dataIcon := "")(trans.cancel())
        )
      )
    }
