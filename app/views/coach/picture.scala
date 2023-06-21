package views.html
package coach

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.coach.Coach

object picture:

  def apply(c: Coach.WithUser, error: Option[String] = None)(using PageContext) =
    views.html.account.layout(
      title = s"${c.user.titleUsername} coach picture",
      evenMoreJs = iifeModule("javascripts/coach.form.js"),
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

  object thumbnail:
    def apply(c: Coach.WithUser, cssSize: Int = Coach.imageSize) =
      img(
        widthA    := Coach.imageSize,
        heightA   := Coach.imageSize,
        cssWidth  := cssSize,
        cssHeight := cssSize,
        cls       := "picture",
        src       := url(c.coach),
        alt       := s"${c.user.titleUsername} Lichess coach picture"
      )
    def url(c: Coach) =
      c.picture match
        case Some(image) => picfitUrl.thumbnail(image, Coach.imageSize, Coach.imageSize)
        case _           => assetUrl("images/placeholder.png")
