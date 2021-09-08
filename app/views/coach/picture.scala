package views.html
package coach

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes
import lila.user.User

object picture {

  def apply(c: lila.coach.Coach.WithUser, error: Option[String] = None)(implicit ctx: Context) =
    views.html.account.layout(
      title = s"${c.user.titleUsername} coach picture",
      evenMoreJs = jsTag("coach.form.js"),
      evenMoreCss = cssTag("coach.editor"),
      active = "coach"
    ) {
      div(cls := "account coach-edit coach-picture box")(
        div(cls := "top")(
          div(cls := "picture_wrap")(
            picture.thumbnail(c, 250)
          ),
          h1(widget.titleName(c))
        ),
        div(cls := "forms")(
          error.map { e =>
            p(cls := "error")(e)
          },
          postForm(action := routes.Coach.pictureApply, enctype := "multipart/form-data", cls := "upload")(
            p("Max size: ", lila.memo.PicfitApi.uploadMaxMb, "MB."),
            form3.file.image("picture"),
            form3.actions(
              a(href := routes.Coach.edit)(trans.cancel()),
              form3.submit("Upload profile picture")
            )
          )
        )
      )
    }

  object thumbnail {
    val size = 350
    def apply(c: lila.coach.Coach.WithUser, cssSize: Int = size) =
      img(
        widthA := size,
        heightA := size,
        width := cssSize,
        height := cssSize,
        cls := "picture",
        src := url(c.coach),
        alt := s"${c.user.titleUsername} Lichess coach picture"
      )
    def url(c: lila.coach.Coach) =
      c.picture match {
        case Some(image) => picfitUrl.thumbnail(image, size, size)
        case _           => assetUrl("images/placeholder.png")
      }
  }
}
