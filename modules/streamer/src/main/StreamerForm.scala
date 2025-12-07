package lila.streamer

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ constraint, partial, given }

object StreamerForm:

  import Streamer.{ Description, Headline, Listed, Name }

  type QuickDecision = "approve" | "decline"

  lazy val emptyUserForm = Form(
    mapping(
      "name" -> nameField,
      "headline" -> optional(of[Headline]),
      "description" -> optional(of[Description]),
      "listed" -> of[Listed],
      "approval" ->
        mapping(
          "granted" -> boolean,
          "tier" -> optional(number(min = 0, max = Streamer.maxTier)),
          "requested" -> boolean,
          "ignored" -> boolean,
          "chat" -> boolean,
          "quick" -> optional:
            text.partial[QuickDecision](_.toString):
              case ok: QuickDecision => ok,
          "reason" -> optional(text)
        )(ApprovalData.apply)(unapply)
    )(UserData.apply)(unapply)
  )

  def userForm(streamer: Streamer) =
    emptyUserForm.fill(
      UserData(
        name = streamer.name,
        headline = streamer.headline,
        description = streamer.description,
        listed = streamer.listed,
        approval = ApprovalData(
          granted = streamer.approval.granted,
          tier = streamer.approval.tier.some,
          requested = streamer.approval.requested,
          ignored = streamer.approval.ignored,
          chat = streamer.approval.chatEnabled
        )
      )
    )

  case class UserData(
      name: Name,
      headline: Option[Headline],
      description: Option[Description],
      listed: Listed,
      approval: ApprovalData
  ):

    def apply(streamer: Streamer, asMod: Boolean) =
      val newApproval =
        if asMod then
          val m = approval.resolve
          streamer.approval.copy(
            granted = m.granted,
            tier = m.tier | streamer.approval.tier,
            requested = m.requested,
            ignored = m.ignored,
            chatEnabled = m.chat,
            reason = if m.granted then none else (~m.reason).some,
            lastGrantedAt = m.granted.option(nowInstant).orElse(streamer.approval.lastGrantedAt)
          )
        else
          streamer.approval.copy(
            requested = streamer.approval.requested || name != streamer.name
              || headline != streamer.headline || description != streamer.description
          )

      streamer.copy(
        name = name,
        headline = headline,
        description = description,
        listed = listed,
        updatedAt = nowInstant,
        approval = newApproval
      )

  case class ApprovalData(
      granted: Boolean,
      tier: Option[Int],
      requested: Boolean,
      ignored: Boolean,
      chat: Boolean,
      quick: Option[QuickDecision] = None,
      reason: Option[String] = None
  ):
    def resolve = quick.fold(this):
      case "approve" => copy(granted = true, requested = false)
      case "decline" => copy(granted = false, requested = false)

  private def nameField = of[Name].verifying(
    constraint.minLength[Name](_.value)(3),
    constraint.maxLength[Name](_.value)(Streamer.maxNameLength)
  )
