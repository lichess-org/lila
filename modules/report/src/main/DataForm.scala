package lila.report

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import lila.user.{ User, UserRepo }

private[report] final class DataForm(val captcher: akka.actor.ActorSelection) extends lila.hub.CaptchedForm {

  val create = Form(mapping(
    "username" -> nonEmptyText.verifying("Unknown username", { fetchUser(_).isDefined }),
    "reason" -> text.verifying("error.required", Reason.keys contains _),
    "text" -> text(minLength = 5, maxLength = 2000),
    "gameId" -> text,
    "move" -> text
  )({
      case (username, reason, text, gameId, move) => ReportSetup(
        user = fetchUser(username) err "Unknown username " + username,
        reason = reason,
        text = text,
        gameId = gameId,
        move = move
      )
    })(_.export.some).verifying(captchaFailMessage, validateCaptcha _))

  def createWithCaptcha = withCaptcha(create)

  private def fetchUser(username: String) = UserRepo named username awaitSeconds 2
}

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
