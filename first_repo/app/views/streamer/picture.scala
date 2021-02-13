package views.html.streamer

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object picture {

  import trans.streamer._

  def apply(s: lila.streamer.Streamer.WithUser, error: Option[String] = None)(implicit ctx: Context) =
    views.html.base.layout(
      title = xStreamerPicture.txt(s.user.username),
      moreJs = embedJsUnsafeLoadThen("""
$('.streamer-picture form.upload input[type=file]').on('change', function() {
  $('.picture_wrap').html(lichess.spinnerHtml);
  $(this).parents('form')[0].submit();
})"""),
      moreCss = cssTag("streamer.form")
    ) {
      main(cls := "streamer-picture small-page box")(
        h1(xStreamerPicture(userLink(s.user))),
        div(cls := "picture_wrap")(bits.pic(s.streamer, s.user, 250)),
        div(cls := "forms")(
          error.map { badTag(_) },
          postForm(
            action := routes.Streamer.pictureApply,
            enctype := "multipart/form-data",
            cls := "upload"
          )(
            p(maxSize(s"${lila.db.Photographer.uploadMaxMb}MB.")),
            form3.file.image("picture"),
            submitButton(cls := "button")(uploadPicture())
          ),
          s.streamer.hasPicture option
            postForm(action := routes.Streamer.pictureDelete, cls := "delete")(
              submitButton(cls := "button button-red")(deletePicture())
            ),
          div(cls := "cancel")(
            a(href := routes.Streamer.edit, cls := "text", dataIcon := "I")(trans.cancel())
          )
        )
      )
    }
}
