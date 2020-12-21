package lila.streamer

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints

import lila.common.Form.{ constraint, formatter }

object StreamerForm {

  import Streamer.{ Description, Headline, Listed, Name, Twitch, YouTube }

  lazy val emptyUserForm = Form(
    mapping(
      "name"        -> nameField,
      "headline"    -> optional(headlineField),
      "description" -> optional(descriptionField),
      "twitch" -> optional(
        text
          .verifying(
            Constraints.minLength(2),
            Constraints.maxLength(47)
          )
          .verifying("Invalid Twitch username", s => Streamer.Twitch.parseUserId(s).isDefined)
      ),
      "youTube" -> optional(
        text.verifying("Invalid YouTube channel", s => Streamer.YouTube.parseChannelId(s).isDefined)
      ),
      "listed" -> boolean,
      "approval" -> optional(
        mapping(
          "granted"   -> boolean,
          "tier"      -> optional(number(min = 0, max = Streamer.maxTier)),
          "requested" -> boolean,
          "ignored"   -> boolean,
          "chat"      -> boolean,
          "quick"     -> optional(nonEmptyText)
        )(ApprovalData.apply)(ApprovalData.unapply)
      )
    )(UserData.apply)(UserData.unapply)
  )

  def userForm(streamer: Streamer) =
    emptyUserForm fill UserData(
      name = streamer.name,
      headline = streamer.headline,
      description = streamer.description,
      twitch = streamer.twitch.map(_.userId),
      youTube = streamer.youTube.map(_.channelId),
      listed = streamer.listed.value,
      approval = ApprovalData(
        granted = streamer.approval.granted,
        tier = streamer.approval.tier.some,
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
        approval = approval.map(_.resolve) match {
          case Some(m) if asMod =>
            streamer.approval.copy(
              granted = m.granted,
              tier = m.tier | streamer.approval.tier,
              requested = !m.granted && {
                if (streamer.approval.requested != m.requested) m.requested
                else streamer.approval.requested || m.requested
              },
              ignored = m.ignored && !m.granted,
              chatEnabled = m.chat,
              lastGrantedAt = m.granted.option(DateTime.now) orElse streamer.approval.lastGrantedAt
            )
          case _ =>
            streamer.approval.copy(
              granted = streamer.approval.granted &&
                newStreamer.twitch.fold(true)(streamer.twitch.has) &&
                newStreamer.youTube.fold(true)(streamer.youTube.has)
            )
        }
      )
    }
  }

  case class ApprovalData(
      granted: Boolean,
      tier: Option[Int],
      requested: Boolean,
      ignored: Boolean,
      chat: Boolean,
      quick: Option[String] = None
  ) {
    def resolve =
      quick.fold(this) {
        case "approve" => copy(granted = true, requested = false)
        case "decline" => copy(granted = false, requested = false)
      }
  }

  implicit private val headlineFormat    = formatter.stringFormatter[Headline](_.value, Headline.apply)
  private def headlineField              = of[Headline].verifying(constraint.maxLength[Headline](_.value)(300))
  implicit private val descriptionFormat = formatter.stringFormatter[Description](_.value, Description.apply)
  private def descriptionField           = of[Description].verifying(constraint.maxLength[Description](_.value)(50000))
  implicit private val nameFormat        = formatter.stringFormatter[Name](_.value, Name.apply)
  private def nameField =
    of[Name].verifying(
      constraint.minLength[Name](_.value)(3),
      constraint.maxLength[Name](_.value)(30)
    )
}
