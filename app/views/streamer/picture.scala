package views.html.streamer

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object picture {

  def apply(s: lila.streamer.Streamer.WithUser, error: Option[String] = None)(implicit ctx: Context) =
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
            st.form(
              action := routes.Streamer.pictureApply,
              method := "post",
              enctype := "multipart/form-data",
              cls := "upload"
            )(
                p("Max size: ", lila.db.Photographer.uploadMaxMb, "MB."),
                form3.file.image("picture"),
                button(tpe := "submit", cls := "button")("Upload profile picture")
              ),
            s.streamer.hasPicture option
              st.form(action := routes.Streamer.pictureDelete, method := "post", cls := "delete")(
                button(tpe := "submit", cls := "button button-red")("Delete profile picture")
              ),
            div(cls := "cancel")(
              a(href := routes.Streamer.edit, cls := "text", dataIcon := "I")("Return to streamer page form")
            )
          )
        )
      }
}
