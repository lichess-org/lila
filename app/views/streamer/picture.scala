package views.html.streamer

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.streamer.Streamer
import lila.user.User

object picture {

  import trans.streamer._

  def apply(s: Streamer.WithUser, error: Option[String] = None)(implicit ctx: Context) =
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
        div(cls := "picture_wrap")(thumbnail(s.streamer, s.user)),
        div(cls := "forms")(
          error.map { badTag(_) },
          postForm(
            action  := routes.Streamer.pictureApply,
            enctype := "multipart/form-data",
            cls     := "upload"
          )(
            p(maxSize(s"${lila.memo.PicfitApi.uploadMaxMb}MB.")),
            form3.file.image("picture"),
            submitButton(cls := "button")(uploadPicture())
          ),
          div(cls := "cancel")(
            a(href := routes.Streamer.edit, cls := "text", dataIcon := "")(trans.cancel())
          )
        )
      )
    }

  object thumbnail {
    def apply(s: Streamer, u: User) =
      img(
        widthA  := Streamer.imageSize,
        heightA := Streamer.imageSize,
        cls     := "picture",
        src     := url(s),
        alt     := s"${u.titleUsername} Lichess streamer picture"
      )
    def url(s: Streamer) =
      s.picture match {
        case Some(image) => picfitUrl.thumbnail(image, Streamer.imageSize, Streamer.imageSize)
        case _           => assetUrl("images/placeholder.png")
      }
  }
}
