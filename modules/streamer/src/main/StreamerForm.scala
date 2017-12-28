package lila.streamer

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

object StreamerForm {

  import Streamer.{ Name, Description, Twitch, YouTube, Live }

  val userForm = Form(mapping(
    "name" -> name,
    "description" -> description,
    "youTube" -> optional(text),
    "twitch" -> optional(text)
  )(UserData.apply)(UserData.unapply))

  case class UserData(
      name: Name,
      description: Description,
      twitch: Option[String],
      youTube: Option[String]
  ) {

    def apply(streamer: Streamer) = streamer.copy(
      name = name.some,
      description = description.some,
      twitch = twitch.flatMap(Twitch.parseUserId).fold(streamer.twitch) { userId =>
        streamer.twitch.fold(Twitch(userId, Live.empty))(_.copy(userId = userId)).some
      },
      youTube = youTube.flatMap(YouTube.parseChannelId).fold(streamer.youTube) { channelId =>
        streamer.youTube.fold(YouTube(channelId, Live.empty))(_.copy(channelId = channelId)).some
      },
      updatedAt = DateTime.now
    )
  }

  private implicit val descriptionFormat = lila.common.Form.formatter.stringFormatter[Description](_.value, Description.apply)
  private def description = of[Description]
  private implicit val nameFormat = lila.common.Form.formatter.stringFormatter[Name](_.value, Name.apply)
  private def name = of[Name]
}
