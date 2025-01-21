package lila.report

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.*

import lila.common.Form.cleanNonEmptyText
import lila.core.LightUser
import lila.core.config.NetDomain
import lila.core.report.SuspectId

final private[report] class ReportForm(lightUserAsync: LightUser.Getter)(using domain: NetDomain):
  val cheatLinkConstraint: Constraint[ReportSetup] = Constraint("constraints.cheatgamelink"): setup =>
    val gameLinkRequired = setup.reason == Reason.Cheat.key || setup.reason == Reason.Stall.key
    if !gameLinkRequired || ReportForm.hasGameLink(setup.text) then Valid
    else Invalid(Seq(ValidationError("error.provideOneCheatedGameLink")))

  def create(using me: MyId) = Form:
    mapping(
      "username" -> lila.common.Form.username.historicalField
        .verifying("Unknown username", { blockingFetchUser(_).isDefined })
        .verifying(
          "You cannot report yourself",
          u => !me.is(u.id)
        )
        .verifying(
          "Don't report Lichess. Use lichess.org/contact instead.",
          u => !UserId.isOfficial(u)
        ),
      "reason" -> text.verifying("error.required", Reason.keys contains _),
      "text"   -> cleanNonEmptyText(minLength = 5),
      "msgs"   -> list(nonEmptyText)
    ) { (username, reason, text, msgs) =>
      ReportSetup(
        user = blockingFetchUser(username).err("Unknown username " + username),
        reason = reason,
        text = text,
        msgs = msgs
      )
    }(_.values.some)
      .verifying(cheatLinkConstraint)
      .verifying(s"Maximum report length is 3000 characters", _.text.length <= 3000)

  val flag = Form:
    mapping(
      "username" -> lila.common.Form.username.historicalField,
      "resource" -> nonEmptyText,
      "text"     -> text(minLength = 1, maxLength = 140)
    )(ReportFlag.apply)(unapply)

  private def blockingFetchUser(username: UserStr) =
    lightUserAsync(username.id).await(1.second, "reportUser")

object ReportForm:
  private def gameLinkRegex(using domain: NetDomain) = (domain.value + """/(\w{8}|\w{12})""").r
  def gameLinks(text: String)(using NetDomain) =
    gameLinkRegex.findAllMatchIn(text).take(20).map(m => GameId(m.group(1))).toList
  def hasGameLink(text: String)(using NetDomain) = gameLinks(text).nonEmpty

private[report] case class ReportFlag(
    username: UserStr,
    resource: String,
    text: String
)

case class ReportSetup(
    user: LightUser,
    reason: String,
    text: String,
    msgs: List[lila.core.msg.ID] = Nil
):
  def suspect = SuspectId(user.id)
  def values  = (user.name.into(UserStr), reason, text, msgs)
