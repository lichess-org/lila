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

  object Fields {
    val name = "name" -> text(minLength = 3, maxLength = 60)
    val location = "location" -> optional(text(minLength = 3, maxLength = 80))
    val description = "description" -> text(minLength = 30, maxLength = 2000)
    val open = "open" -> number
    val gameId = "gameId" -> text
    val move = "move" -> text
  }

  val create = Form(mapping(
    Fields.name,
    Fields.location,
    Fields.description,
    Fields.open,
    Fields.gameId,
    Fields.move)(TeamSetup.apply)(TeamSetup.unapply)
    .verifying("This team already exists", d ⇒ !teamExists(d))
    .verifying(
      "Not a checkmate",
      data ⇒ captcher get data.gameId valid data.move.trim.toLowerCase
    )
  )

  def edit(team: Team) = Form(mapping(
    Fields.location,
    Fields.description,
    Fields.open)(TeamEdit.apply)(TeamEdit.unapply)) fill TeamEdit(
    location = team.location,
    description = team.description,
    open = team.open.fold(1, 0))


  val request = Form(mapping(
    "message" -> text(minLength = 30, maxLength = 2000),
    Fields.gameId,
    Fields.move
  )(RequestSetup.apply)(RequestSetup.unapply)
    .verifying(
      "Not a checkmate",
      data ⇒ captcher get data.gameId valid data.move.trim.toLowerCase
    )
  ) fill RequestSetup(
    message = "Hello, I would like to join the team!",
    gameId = "",
    move = "")

  val processRequest = Form(single(
    "process" -> nonEmptyText
  ))

  val kick = Form(single(
    "userId" -> nonEmptyText
  ))

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
    location = location map (_.trim) filter (_.nonEmpty),
    description = description.trim)
}

private[team] case class TeamEdit(
    location: Option[String],
    description: String,
    open: Int) {

  def isOpen = open == 1

  def trim = copy(
    location = location map (_.trim) filter (_.nonEmpty),
    description = description.trim)
}

private[team] case class RequestSetup(
  message: String,
  gameId: String,
  move: String)
