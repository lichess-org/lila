package views.html.streamer

import controllers.routes
import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

object picture {

  def apply(s: lidraughts.streamer.Streamer.WithUser, error: Option[String] = None)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${s.user.titleUsername} streamer picture",
      moreJs = jsTag("streamer.form.js"),
      moreCss = cssTag("streamer.form")
    ) {
        main(cls := "streamer-picture small-page box")(
          h1(userLink(s.user), " streamer picture"),
          div(cls := "picture_wrap")(bits.pic(s.streamer, s.user, 250)),
          div(cls := "forms")(
            error.map { badTag(_) },
            postForm(
              action := routes.Streamer.pictureApply,
              enctype := "multipart/form-data",
              cls := "upload"
            )(
                p("Max size: ", lidraughts.db.Photographer.uploadMaxMb, "MB."),
                form3.file.image("picture"),
                submitButton(cls := "button")("Upload profile picture")
              ),
            s.streamer.hasPicture option
              postForm(action := routes.Streamer.pictureDelete, cls := "delete")(
                submitButton(cls := "button button-red")("Delete profile picture")
              ),
            div(cls := "cancel")(
              a(href := routes.Streamer.edit, cls := "text", dataIcon := "I")("Return to streamer page form")
            )
          )
        )
      }
}
