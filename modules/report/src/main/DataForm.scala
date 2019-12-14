package lila.report

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import scala.concurrent.duration._

import lila.user.{ User, UserRepo }

final private[report] class DataForm(
    userRepo: UserRepo,
    val captcher: lila.hub.actors.Captcher,
    domain: lila.common.config.NetDomain
)(implicit ec: scala.concurrent.ExecutionContext)
    extends lila.hub.CaptchedForm {
  val cheatLinkConstraint: Constraint[ReportSetup] = Constraint("constraints.cheatgamelink")({ setup =>
    if (setup.reason != "cheat" || (domain.value + """/(\w{8}|\w{12})""").r.findFirstIn(setup.text).isDefined)
      Valid
    else
      Invalid(Seq(ValidationError("error.provideOneCheatedGameLink")))
  })

  val create = Form(
    mapping(
      "username" -> lila.user.DataForm.historicalUsernameField.verifying("Unknown username", {
        blockingFetchUser(_).isDefined
      }),
      "reason" -> text.verifying("error.required", Reason.keys contains _),
      "text"   -> text(minLength = 5, maxLength = 2000),
      "gameId" -> text,
      "move"   -> text
    )({
      case (username, reason, text, gameId, move) =>
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
      "username" -> lila.user.DataForm.historicalUsernameField,
      "resource" -> nonEmptyText,
      "text"     -> text(minLength = 3, maxLength = 140)
    )(ReportFlag.apply)(ReportFlag.unapply)
  )

  private def blockingFetchUser(username: String) =
    userRepo.named(username).await(1.second, "reportUser")
}

private[report] case class ReportFlag(
    username: String,
    resource: String,
    text: String
)

private[report] case class ReportSetup(
    user: User,
    reason: String,
    text: String,
    gameId: String,
    move: String
) {

  def suspect = Suspect(user)

  def export = (user.username, reason, text, gameId, move)

  def candidate(reporter: Reporter) = {
    Report.Candidate(reporter, suspect, Reason(reason) err s"Invalid report reason ${reason}", text)
  }
}
