package lila.streamer

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

object StreamerForm {

  import Streamer.{ Name, Headline, Description, Twitch, YouTube, Listed }

  lazy val emptyUserForm = Form(mapping(
    "name" -> name,
    "headline" -> optional(headline),
    "description" -> optional(description),
    "twitch" -> optional(nonEmptyText.verifying("Invalid Twitch username", s => Streamer.Twitch.parseUserId(s).isDefined)),
    "youTube" -> optional(nonEmptyText.verifying("Invalid YouTube channel", s => Streamer.YouTube.parseChannelId(s).isDefined)),
    "listed" -> boolean,
    "approval" -> optional(mapping(
      "granted" -> boolean,
      "featured" -> boolean,
      "requested" -> boolean,
      "ignored" -> boolean,
      "chat" -> boolean
    )(ApprovalData.apply)(ApprovalData.unapply))
  )(UserData.apply)(UserData.unapply))

  def userForm(streamer: Streamer) = emptyUserForm fill UserData(
    name = streamer.name,
    headline = streamer.headline,
    description = streamer.description,
    twitch = streamer.twitch.map(_.userId),
    youTube = streamer.youTube.map(_.channelId),
    listed = streamer.listed.value,
    approval = ApprovalData(
      granted = streamer.approval.granted,
      featured = streamer.approval.autoFeatured,
      requested = streamer.approval.requested,
      ignored = streamer.approval.ignored,
      chat = streamer.approval.chatEnabled
    ).some
  )

  case class UserData(
      name: Name,
      headline: Option[Headline],
      description: Option[Description],
      twitch: Option[String],
      youTube: Option[String],
      listed: Boolean,
      approval: Option[ApprovalData]
  ) {

    def apply(streamer: Streamer, asMod: Boolean) = {
      val newStreamer = streamer.copy(
        name = name,
        headline = headline,
        description = description,
        twitch = twitch.flatMap(Twitch.parseUserId).map(Twitch.apply),
        youTube = youTube.flatMap(YouTube.parseChannelId).map(YouTube.apply),
        listed = Listed(listed),
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
            ignored = m.ignored && !m.granted,
            chatEnabled = m.chat
          )
          case None => streamer.approval
        }
      )
    }
  }

  case class ApprovalData(
      granted: Boolean,
      featured: Boolean,
      requested: Boolean,
      ignored: Boolean,
      chat: Boolean
  )

  private implicit val headlineFormat = lila.common.Form.formatter.stringFormatter[Headline](_.value, Headline.apply)
  private def headline = of[Headline]
  private implicit val descriptionFormat = lila.common.Form.formatter.stringFormatter[Description](_.value, Description.apply)
  private def description = of[Description]
  private implicit val nameFormat = lila.common.Form.formatter.stringFormatter[Name](_.value, Name.apply)
  private def name = of[Name]
}
