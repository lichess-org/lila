package lila.report

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import scala.concurrent.duration._

import lila.common.LightUser
import lila.user.User

final private[report] class ReportForm(
    lightUserAsync: LightUser.Getter,
    val captcher: lila.hub.actors.Captcher,
    domain: lila.common.config.NetDomain
)(implicit ec: scala.concurrent.ExecutionContext)
    extends lila.hub.CaptchedForm {
  val cheatLinkConstraint: Constraint[ReportSetup] = Constraint("constraints.cheatgamelink") { setup =>
    if (setup.reason != "cheat" || (domain.value + """/(\w{8}|\w{12})""").r.findFirstIn(setup.text).isDefined)
      Valid
    else
      Invalid(Seq(ValidationError("error.provideOneCheatedGameLink")))
  }

  val create = Form(
    mapping(
      "username" -> lila.user.UserForm.historicalUsernameField
        .verifying("Unknown username", { blockingFetchUser(_).isDefined })
        .verifying(
          "Don't report Lichess. Use lichess.org/contact instead.",
          u => !User.isOfficial(u)
        ),
      "reason" -> text.verifying("error.required", Reason.keys contains _),
      "text"   -> text(minLength = 5, maxLength = 2000),
      "gameId" -> text,
      "move"   -> text
    )({ case (username, reason, text, gameId, move) =>
      ReportSetup(
        user = blockingFetchUser(username) err "Unknown username " + username,
        reason = reason,
        text = text,
        gameId = gameId,
        move = move
      )
    })(_.export.some).verifying(captchaFailMessage, validateCaptcha _).verifying(cheatLinkConstraint)
  )

  def createWithCaptcha = withCaptcha(create)

  val flag = Form(
    mapping(
      "username" -> lila.user.UserForm.historicalUsernameField,
      "resource" -> nonEmptyText,
      "text"     -> text(minLength = 3, maxLength = 140)
    )(ReportFlag.apply)(ReportFlag.unapply)
  )

  private def blockingFetchUser(username: String) =
    lightUserAsync(User normalize username).await(1 second, "reportUser")
}

private[report] case class ReportFlag(
    username: String,
    resource: String,
    text: String
)

case class ReportSetup(
    user: LightUser,
    reason: String,
    text: String,
    gameId: String,
    move: String
) {

  def suspect = SuspectId(user.id)

  def export = (user.name, reason, text, gameId, move)
}
