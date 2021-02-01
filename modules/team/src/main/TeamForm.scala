package lila.team

import play.api.data._
import play.api.data.Forms._
import scala.concurrent.duration._

import lila.common.Form.{ cleanText, numberIn }
import lila.db.dsl._

final private[team] class TeamForm(
    teamRepo: TeamRepo,
    lightUserApi: lila.user.LightUserApi,
    val captcher: lila.hub.actors.Captcher
)(implicit ec: scala.concurrent.ExecutionContext)
    extends lila.hub.CaptchedForm {

  private object Fields {
    val name     = "name"     -> cleanText(minLength = 3, maxLength = 60)
    val location = "location" -> optional(cleanText(minLength = 3, maxLength = 80))
    val password = "password" -> optional(cleanText(maxLength = 60))
    def passwordCheck(team: Team) = "password" -> optional(text).verifying(
      "team:incorrectTeamPassword",
      pw => team.password.fold(true)(_ == pw.??(_.trim))
    )
    def requestMessage(team: Team) =
      "message" -> optional(cleanText(minLength = 30, maxLength = 2000))
        .verifying("Request message required", msg => msg.isDefined || team.open)
    val description = "description" -> cleanText(minLength = 30, maxLength = 2000)
    val request     = "request"     -> boolean
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
      Fields.request,
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
        Fields.request,
        Fields.chat
      )(TeamEdit.apply)(TeamEdit.unapply)
    ) fill TeamEdit(
      location = team.location,
      password = team.password,
      description = team.description,
      request = !team.open,
      chat = team.chat
    )

  def request(team: Team) = Form(
    mapping(
      Fields.requestMessage(team),
      Fields.passwordCheck(team)
    )(RequestSetup.apply)(RequestSetup.unapply)
  ) fill RequestSetup(
    message = "Hello, I would like to join the team!".some,
    password = None
  )

  def apiRequest(team: Team) = Form(
    mapping(
      Fields.requestMessage(team),
      Fields.passwordCheck(team)
    )(RequestSetup.apply)(RequestSetup.unapply)
  )

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
    single("message" -> cleanText(minLength = 3, maxLength = 9000))
  )

  def leaders(t: Team) =
    Form(single("leaders" -> nonEmptyText)) fill t.leaders
      .flatMap(lightUserApi.sync)
      .map(_.name)
      .mkString(", ")

  private def teamExists(setup: TeamSetup) =
    teamRepo.coll.exists($id(Team nameToId setup.trim.name))
}

private[team] case class TeamSetup(
    name: String,
    location: Option[String],
    password: Option[String],
    description: String,
    request: Boolean,
    gameId: String,
    move: String
) {

  def isOpen = !request

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
    request: Boolean,
    chat: Team.ChatFor
) {

  def isOpen = !request

  def trim =
    copy(
      location = location map (_.trim) filter (_.nonEmpty),
      description = description.trim
    )
}

private[team] case class RequestSetup(
    message: Option[String],
    password: Option[String]
)
