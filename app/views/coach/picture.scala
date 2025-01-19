package views.html
package coach

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object picture {

  def apply(c: lila.coach.Coach.WithUser, error: Option[String] = None)(implicit ctx: Context) =
    views.html.account.layout(
      title = s"${c.user.titleUsername} coach picture",
      evenMoreCss = cssTag("misc.coach.editor"),
      evenMoreJs = jsTag("misc.coach-form"),
      active = "coach",
    ) {
      div(cls := "account coach-edit coach-picture box")(
        div(cls := "top")(
          div(cls := "picture_wrap")(
            widget.pic(c, 250),
          ),
          h1(widget.titleName(c)),
        ),
        div(cls := "forms")(
          error.map { e =>
            p(cls := "error")(e)
          },
          postForm(
            action  := routes.Coach.pictureApply,
            enctype := "multipart/form-data",
            cls     := "upload",
          )(
            p("Max size: ", lila.db.Photographer.uploadMaxMb, "MB."),
            form3.file.image("picture"),
            form3.actions(
              a(href := routes.Coach.edit)(trans.cancel()),
              form3.submit("Upload profile picture"),
            ),
          ),
          c.coach.hasPicture option
            st.form(action := routes.Coach.pictureDelete, cls := "delete")(
              submitButton(cls := "confirm button button-empty button-red")(
                "Delete profile picture",
              ),
            ),
        ),
      )
    }
}
