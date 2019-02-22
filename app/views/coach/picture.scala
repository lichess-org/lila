package views.html
package coach

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object picture {

  def apply(c: lila.coach.Coach.WithUser, error: Option[String] = None)(implicit ctx: Context) =
    views.html.account.layout(
      title = s"${c.user.titleUsername} coach picture",
      evenMoreJs = jsTag("coach.form.js"),
      evenMoreCss = responsiveCssTag("coach.editor"),
      active = "coach"
    ) {
        div(cls := "account coach-edit coach-picture box")(
          div(cls := "top")(
            div(cls := "picture_wrap")(
              widget.pic(c, 250)
            ),
            h1(widget.titleName(c))
          ),
          div(cls := "forms")(
            error.map { e =>
              p(cls := "error")(e)
            },
            st.form(action := routes.Coach.pictureApply, enctype := "multipart/form-data", cls := "upload")(
              p("Max size: ", lila.db.Photographer.uploadMaxMb, "MB."),
              form3.file.image("picture"),
              button(tpe := "submit", cls := "button")("Upload profile picture")
            ),
            c.coach.hasPicture option
              st.form(action := routes.Coach.pictureDelete, cls := "delete")(
                button(tpe := "submit", cls := "confirm button button-empty button-red")("Delete profile picture")
              )
          )
        )
      }
}
