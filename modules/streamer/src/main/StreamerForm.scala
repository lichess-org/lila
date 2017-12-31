package lila.streamer

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

object StreamerForm {

  import Streamer.{ Name, Description, Twitch, YouTube, Live }

  lazy val emptyUserForm = Form(mapping(
    "name" -> name,
    "description" -> optional(description),
    "twitch" -> optional(nonEmptyText.verifying("Invalid Twitch username", s => Streamer.Twitch.parseUserId(s).isDefined)),
    "youTube" -> optional(nonEmptyText.verifying("Invalid YouTube channel", s => Streamer.YouTube.parseChannelId(s).isDefined)),
    "approval" -> optional(mapping(
      "granted" -> boolean,
      "featured" -> boolean,
      "requested" -> boolean,
      "ignored" -> boolean
    )(ApprovalData.apply)(ApprovalData.unapply))
  )(UserData.apply)(UserData.unapply))

  def userForm(streamer: Streamer) = emptyUserForm fill UserData(
    name = streamer.name,
    description = streamer.description,
    twitch = streamer.twitch.map(_.userId),
    youTube = streamer.youTube.map(_.channelId),
    approval = ApprovalData(
      granted = streamer.approval.granted,
      featured = streamer.approval.autoFeatured,
      requested = streamer.approval.requested,
      ignored = streamer.approval.ignored
    ).some
  )

  case class UserData(
      name: Name,
      description: Option[Description],
      twitch: Option[String],
      youTube: Option[String],
      approval: Option[ApprovalData]
  ) {

    def apply(streamer: Streamer, asMod: Boolean) = {
      val newStreamer = streamer.copy(
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
      newStreamer.copy(
        approval = approval match {
          case Some(m) if asMod => streamer.approval.copy(
            granted = m.granted,
            autoFeatured = m.featured && m.granted,
            requested = !m.granted && {
              if (streamer.approval.requested != m.requested) m.requested
              else streamer.approval.requested || m.requested
            },
            ignored = m.ignored && !m.granted
          )
          case None if streamer.twitch != newStreamer.twitch || streamer.youTube != newStreamer.youTube =>
            streamer.approval.copy(granted = false, autoFeatured = false)
          case None => streamer.approval
        }
      )
    }
  }

  case class ApprovalData(
      granted: Boolean,
      featured: Boolean,
      requested: Boolean,
      ignored: Boolean
  )

  private implicit val descriptionFormat = lila.common.Form.formatter.stringFormatter[Description](_.value, Description.apply)
  private def description = of[Description]
  private implicit val nameFormat = lila.common.Form.formatter.stringFormatter[Name](_.value, Name.apply)
  private def name = of[Name]
}
