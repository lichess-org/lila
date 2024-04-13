package views.html
package coach
import lila.app.templating.Environment.*
import lila.web.ui.ScalatagsTemplate.*
import lila.coach.Coach

object picture:

  object thumbnail:
    def apply(c: Coach.WithUser, cssSize: Int = Coach.imageSize) =
      img(
        widthA  := Coach.imageSize,
        heightA := Coach.imageSize,
        cls     := "picture",
        src     := url(c.coach),
        alt     := s"${c.user.titleUsername} Lichess coach picture"
      )
    def url(c: Coach) =
      c.picture match
        case Some(image) => picfitUrl.thumbnail(image, Coach.imageSize, Coach.imageSize)
        case _           => assetUrl("images/placeholder.png")
