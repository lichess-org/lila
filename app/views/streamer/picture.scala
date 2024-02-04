package views.html.streamer

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.streamer.Streamer
import lila.user.User

object picture:

  import trans.streamer.*
  object thumbnail:
    def apply(s: Streamer, u: User) =
      img(
        widthA  := Streamer.imageSize,
        heightA := Streamer.imageSize,
        cls     := "picture",
        src     := url(s),
        alt     := s"${u.titleUsername} Lichess streamer picture"
      )
    def url(s: Streamer) =
      s.picture match
        case Some(image) => picfitUrl.thumbnail(image, Streamer.imageSize, Streamer.imageSize)
        case _           => assetUrl("images/placeholder.png")
