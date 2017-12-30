package lila.streamer

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

object StreamerForm {

  import Streamer.{ Name, Description, Twitch, YouTube, Live }

  lazy val emptyUserForm = Form(mapping(
    "name" -> optional(name),
    "description" -> optional(description),
    "youTube" -> optional(text),
    "twitch" -> optional(text)
  )(UserData.apply)(UserData.unapply))

  def userForm(streamer: Streamer) = emptyUserForm fill UserData(
    name = streamer.name,
    description = streamer.description,
    twitch = streamer.twitch.map(_.userId),
    youTube = streamer.youTube.map(_.channelId)
  )

  case class UserData(
      name: Option[Name],
      description: Option[Description],
      twitch: Option[String],
      youTube: Option[String]
  ) {

    def apply(streamer: Streamer) = streamer.copy(
      name = name,
      description = description,
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
