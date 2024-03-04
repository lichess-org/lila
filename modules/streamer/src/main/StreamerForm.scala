package lila.streamer

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.Constraints

import lila.common.Form.{ constraint, given }

object StreamerForm:

  import Streamer.{ Description, Headline, Listed, Name, Twitch, YouTube }

  lazy val emptyUserForm = Form(
    mapping(
      "name"        -> nameField,
      "headline"    -> optional(of[Headline]),
      "description" -> optional(of[Description]),
      "twitch" -> optional(
        text
          .verifying(
            Constraints.minLength(2),
            Constraints.maxLength(47)
          )
          .verifying("Invalid Twitch username", s => Streamer.Twitch.parseUserId(s).isDefined)
      ),
      "youTube" -> optional(
        text.verifying("Invalid YouTube channel ID", s => Streamer.YouTube.parseChannelId(s).isDefined)
      ),
      "listed" -> of[Listed],
      "approval" -> optional(
        mapping(
          "granted"   -> boolean,
          "tier"      -> optional(number(min = 0, max = Streamer.maxTier)),
          "requested" -> boolean,
          "ignored"   -> boolean,
          "chat"      -> boolean,
          "quick"     -> optional(nonEmptyText)
        )(ApprovalData.apply)(unapply)
      )
    )(UserData.apply)(unapply)
      .verifying(
        "Must specify a Twitch and/or YouTube channel.",
        u => u.twitch.isDefined || u.youTube.isDefined
      )
  )

  def userForm(streamer: Streamer) =
    emptyUserForm.fill(
      UserData(
        name = streamer.name,
        headline = streamer.headline,
        description = streamer.description,
        twitch = streamer.twitch.map(_.userId),
        youTube = streamer.youTube.map(_.channelId),
        listed = streamer.listed,
        approval = ApprovalData(
          granted = streamer.approval.granted,
          tier = streamer.approval.tier.some,
          requested = streamer.approval.requested,
          ignored = streamer.approval.ignored,
          chat = streamer.approval.chatEnabled
        ).some
      )
    )

  case class UserData(
      name: Name,
      headline: Option[Headline],
      description: Option[Description],
      twitch: Option[String],
      youTube: Option[String],
      listed: Listed,
      approval: Option[ApprovalData]
  ):

    def apply(streamer: Streamer, asMod: Boolean) =
      val liveVideoId   = streamer.youTube.flatMap(_.liveVideoId)
      val pubsubVideoId = streamer.youTube.flatMap(_.pubsubVideoId)
      val newStreamer = streamer.copy(
        name = name,
        headline = headline,
        description = description,
        twitch = twitch.flatMap(Twitch.parseUserId).map(Twitch.apply),
        youTube = youTube.flatMap(YouTube.parseChannelId).map(YouTube.apply(_, liveVideoId, pubsubVideoId)),
        listed = listed,
        updatedAt = nowInstant
      )
      newStreamer.copy(
        approval = approval.map(_.resolve) match
          case Some(m) if asMod =>
            streamer.approval.copy(
              granted = m.granted,
              tier = m.tier | streamer.approval.tier,
              requested = !m.granted && {
                if streamer.approval.requested != m.requested then m.requested
                else streamer.approval.requested || m.requested
              },
              ignored = m.ignored && !m.granted,
              chatEnabled = m.chat,
              lastGrantedAt = m.granted.option(nowInstant).orElse(streamer.approval.lastGrantedAt)
            )
          case _ =>
            streamer.approval.copy(
              granted = streamer.approval.granted &&
                newStreamer.twitch.forall(streamer.twitch.has) &&
                newStreamer.youTube.forall(streamer.youTube.has)
            )
      )

  case class ApprovalData(
      granted: Boolean,
      tier: Option[Int],
      requested: Boolean,
      ignored: Boolean,
      chat: Boolean,
      quick: Option[String] = None
  ):
    def resolve =
      quick.fold(this) {
        case "approve" => copy(granted = true, requested = false)
        case "decline" => copy(granted = false, requested = false)
      }

  private def nameField = of[Name].verifying(
    constraint.minLength[Name](_.value)(3),
    constraint.maxLength[Name](_.value)(30)
  )
