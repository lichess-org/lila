package lila.report

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.*

import lila.common.{ config, LightUser }
import lila.common.Form.given
import lila.user.{ User, Me }

final private[report] class ReportForm(
    lightUserAsync: LightUser.Getter,
    domain: config.NetDomain
):
  val cheatLinkConstraint: Constraint[ReportSetup] = Constraint("constraints.cheatgamelink"): setup =>
    if setup.reason != "cheat" || ReportForm.gameLinkRegex(domain).findFirstIn(setup.text).isDefined
    then Valid
    else Invalid(Seq(ValidationError("error.provideOneCheatedGameLink")))

  def create(using me: Me) = Form:
    mapping(
      "username" -> lila.user.UserForm.historicalUsernameField
        .verifying("Unknown username", { blockingFetchUser(_).isDefined })
        .verifying(
          "You cannot report yourself",
          u => !me.is(u.id)
        )
        .verifying(
          "Don't report Lichess. Use lichess.org/contact instead.",
          u => !User.isOfficial(u)
        ),
      "reason" -> text.verifying("error.required", Reason.keys contains _),
      "text"   -> text(minLength = 5, maxLength = 2000)
    ) { (username, reason, text) =>
      ReportSetup(
        user = blockingFetchUser(username) err "Unknown username " + username,
        reason = reason,
        text = text
      )
    }(_.values.some)
      .verifying(cheatLinkConstraint)

  val flag = Form:
    mapping(
      "username" -> lila.user.UserForm.historicalUsernameField,
      "resource" -> nonEmptyText,
      "text"     -> text(minLength = 3, maxLength = 140)
    )(ReportFlag.apply)(unapply)

  private def blockingFetchUser(username: UserStr) =
    lightUserAsync(username.id).await(1 second, "reportUser")

object ReportForm:
  def gameLinkRegex(domain: config.NetDomain) = (domain.value + """/(\w{8}|\w{12})""").r

private[report] case class ReportFlag(
    username: UserStr,
    resource: String,
    text: String
)

case class ReportSetup(
    user: LightUser,
    reason: String,
    text: String
):

  def suspect = SuspectId(user.id)

  def values = (user.name into UserStr, reason, text)
