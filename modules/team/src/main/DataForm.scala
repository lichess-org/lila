package lila.team

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import lila.db.dsl._

private[team] final class DataForm(
    teamColl: Coll,
    val captcher: akka.actor.ActorSelection
) extends lila.hub.CaptchedForm {

  private object Fields {
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
    Fields.move
  )(TeamSetup.apply)(TeamSetup.unapply)
    .verifying("This team already exists", d => !teamExists(d).awaitSeconds(2))
    .verifying(captchaFailMessage, validateCaptcha _))

  def edit(team: Team) = Form(mapping(
    Fields.location,
    Fields.description,
    Fields.open
  )(TeamEdit.apply)(TeamEdit.unapply)) fill TeamEdit(
    location = team.location,
    description = team.description,
    open = if (team.open) 1 else 0
  )

  val request = Form(mapping(
    "message" -> text(minLength = 30, maxLength = 2000),
    Fields.gameId,
    Fields.move
  )(RequestSetup.apply)(RequestSetup.unapply)
    .verifying(captchaFailMessage, validateCaptcha _)) fill RequestSetup(
    message = "Hello, I would like to join the team!",
    gameId = "",
    move = ""
  )

  val processRequest = Form(tuple(
    "process" -> nonEmptyText,
    "url" -> nonEmptyText
  ))

  val selectMember = Form(single(
    "userId" -> lila.user.DataForm.historicalUsernameField
  ))

  def createWithCaptcha = withCaptcha(create)

  private def teamExists(setup: TeamSetup) =
    teamColl.exists($id(Team nameToId setup.trim.name))
}

private[team] case class TeamSetup(
    name: String,
    location: Option[String],
    description: String,
    open: Int,
    gameId: String,
    move: String
) {

  def isOpen = open == 1

  def trim = copy(
    name = name.trim,
    location = location map (_.trim) filter (_.nonEmpty),
    description = description.trim
  )
}

private[team] case class TeamEdit(
    location: Option[String],
    description: String,
    open: Int
) {

  def isOpen = open == 1

  def trim = copy(
    location = location map (_.trim) filter (_.nonEmpty),
    description = description.trim
  )
}

private[team] case class RequestSetup(
    message: String,
    gameId: String,
    move: String
)
