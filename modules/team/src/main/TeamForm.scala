package lila.team

import play.api.data._
import play.api.data.Forms._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.common.Form.{ clean, numberIn }

final private[team] class TeamForm(
    teamRepo: TeamRepo,
    lightUserApi: lila.user.LightUserApi,
    val captcher: lila.hub.actors.Captcher
)(implicit ec: scala.concurrent.ExecutionContext)
    extends lila.hub.CaptchedForm {

  private object Fields {
    val name        = "name"        -> clean(text(minLength = 3, maxLength = 60))
    val location    = "location"    -> optional(clean(text(minLength = 3, maxLength = 80)))
    val password    = "password"    -> optional(clean(text(maxLength = 60)))
    val description = "description" -> clean(text(minLength = 30, maxLength = 2000))
    val open        = "open"        -> number
    val gameId      = "gameId"      -> text
    val move        = "move"        -> text
    val chat        = "chat"        -> numberIn(Team.ChatFor.all)
  }

  val create = Form(
    mapping(
      Fields.name,
      Fields.location,
      Fields.password,
      Fields.description,
      Fields.open,
      Fields.gameId,
      Fields.move
    )(TeamSetup.apply)(TeamSetup.unapply)
      .verifying("team:teamAlreadyExists", d => !teamExists(d).await(2 seconds, "teamExists"))
      .verifying(captchaFailMessage, validateCaptcha _)
  )

  def edit(team: Team) =
    Form(
      mapping(
        Fields.location,
        Fields.password,
        Fields.description,
        Fields.open,
        Fields.chat
      )(TeamEdit.apply)(TeamEdit.unapply)
    ) fill TeamEdit(
      location = team.location,
      password = team.password,
      description = team.description,
      open = if (team.open) 1 else 0,
      chat = team.chat
    )

  def request(team: Team) = Form(
    mapping(
      "message"  -> clean(text(minLength = 30, maxLength = 2000)),
      "password" -> text()
    )(RequestSetup.apply)(RequestSetup.unapply).verifying(
      "team:incorrectTeamPassword",
      d => passwordMatches(d, team.password).await(2 seconds, "passwordMatches")
    )
  ) fill RequestSetup(
    message = "Hello, I would like to join the team!",
    password = ""
  )

  val apiRequest = Form(single("message" -> optional(clean(text(minLength = 30, maxLength = 2000)))))

  val processRequest = Form(
    tuple(
      "process" -> nonEmptyText,
      "url"     -> nonEmptyText
    )
  )

  val selectMember = Form(
    single(
      "userId" -> lila.user.UserForm.historicalUsernameField
    )
  )

  def createWithCaptcha = withCaptcha(create)

  val pmAll = Form(
    single("message" -> clean(text(minLength = 3, maxLength = 9000)))
  )

  def leaders(t: Team) =
    Form(single("leaders" -> nonEmptyText)) fill t.leaders
      .flatMap(lightUserApi.sync)
      .map(_.name)
      .mkString(", ")

  private def teamExists(setup: TeamSetup) =
    teamRepo.coll.exists($id(Team nameToId setup.trim.name))

  private def passwordMatches(setup: RequestSetup, password: Option[String]) = {
    if (password == None) fuTrue
    else if (password.get == setup.password) fuTrue
    else
      fuFalse
  }
}

private[team] case class TeamSetup(
    name: String,
    location: Option[String],
    password: Option[String],
    description: String,
    open: Int,
    gameId: String,
    move: String
) {

  def isOpen = open == 1

  def trim =
    copy(
      name = name.trim,
      location = location map (_.trim) filter (_.nonEmpty),
      description = description.trim
    )
}

private[team] case class TeamEdit(
    location: Option[String],
    password: Option[String],
    description: String,
    open: Int,
    chat: Team.ChatFor
) {

  def isOpen = open == 1

  def trim =
    copy(
      location = location map (_.trim) filter (_.nonEmpty),
      description = description.trim
    )
}

private[team] case class RequestSetup(
    message: String,
    password: String
)
