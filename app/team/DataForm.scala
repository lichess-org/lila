package lila
package team

import site.Captcha

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

final class DataForm(
    repo: TeamRepo,
    captcher: Captcha) {

  import lila.core.Form._

  val create = Form(mapping(
    "name" -> text(minLength = 3, maxLength = 60),
    "location" -> optional(text(minLength = 3, maxLength = 80)),
    "description" -> text(minLength = 30, maxLength = 2000),
    "open" -> number,
    "gameId" -> text,
    "move" -> text
  )(TeamSetup.apply)(TeamSetup.unapply)
    .verifying("This team already exists", d ⇒ !teamExists(d))
    .verifying(
      "Not a checkmate",
      data ⇒ captcher get data.gameId valid data.move.trim.toLowerCase
    )
  )

  val request = Form(mapping(
    "message" -> text(minLength = 30, maxLength = 2000),
    "gameId" -> text,
    "move" -> text
  )(RequestSetup.apply)(RequestSetup.unapply)
    .verifying(
      "Not a checkmate",
      data ⇒ captcher get data.gameId valid data.move.trim.toLowerCase
    )
  )

  def createWithCaptcha = create -> captchaCreate

  def captchaCreate: Captcha.Challenge = captcher.create

  private def teamExists(setup: TeamSetup) =
    repo.exists(Team nameToId setup.trim.name).unsafePerformIO
}

private[team] case class TeamSetup(
    name: String,
    location: Option[String],
    description: String,
    open: Int,
    gameId: String,
    move: String) {

  def isOpen = open == 1

  def trim = copy(
    name = name.trim,
    location = location map (_.trim),
    description = description.trim)
}

private[team] case class RequestSetup(
    message: String,
    gameId: String,
    move: String) 
