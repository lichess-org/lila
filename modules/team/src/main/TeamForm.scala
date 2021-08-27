package lila.team

import play.api.data._
import play.api.data.Forms._
import scala.concurrent.duration._

import lila.common.Form.{ cleanNonEmptyText, cleanText, mustNotContainLichess, numberIn }
import lila.db.dsl._

final private[team] class TeamForm(
    teamRepo: TeamRepo,
    lightUserApi: lila.user.LightUserApi,
    val captcher: lila.hub.actors.Captcher
)(implicit ec: scala.concurrent.ExecutionContext)
    extends lila.hub.CaptchedForm {

  private object Fields {
    val name     = "name"     -> cleanText(minLength = 3, maxLength = 60).verifying(mustNotContainLichess(false))
    val password = "password" -> optional(cleanText(maxLength = 60))
    def passwordCheck(team: Team) = "password" -> optional(text).verifying(
      "team:incorrectEntryCode",
      pw => team.passwordMatches(pw.??(_.trim))
    )
    def requestMessage(team: Team) =
      "message" -> optional(cleanText(minLength = 30, maxLength = 2000))
        .verifying("Request message required", msg => msg.isDefined || team.open)
    val description = "description" -> cleanText(minLength = 30, maxLength = 4000)
    val descPrivate = "descPrivate" -> optional(cleanNonEmptyText(maxLength = 4000))
    val request     = "request"     -> boolean
    val gameId      = "gameId"      -> text
    val move        = "move"        -> text
    val chat        = "chat"        -> numberIn(Team.Access.all)
    val forum       = "forum"       -> numberIn(Team.Access.all)
    val hideMembers = "hideMembers" -> boolean
  }

  val create = Form(
    mapping(
      Fields.name,
      Fields.password,
      Fields.description,
      Fields.descPrivate,
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
        Fields.password,
        Fields.description,
        Fields.descPrivate,
        Fields.request,
        Fields.chat,
        Fields.forum,
        Fields.hideMembers
      )(TeamEdit.apply)(TeamEdit.unapply)
    ) fill TeamEdit(
      password = team.password,
      description = team.description,
      descPrivate = team.descPrivate,
      request = !team.open,
      chat = team.chat,
      forum = team.forum,
      hideMembers = team.hideMembers.has(true)
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

  def members = Form(
    single("members" -> nonEmptyText)
  )

  private def teamExists(setup: TeamSetup) =
    teamRepo.coll.exists($id(Team nameToId setup.name))
}

private[team] case class TeamSetup(
    name: String,
    password: Option[String],
    description: String,
    descPrivate: Option[String],
    request: Boolean,
    gameId: String,
    move: String
) {
  def isOpen = !request
}

private[team] case class TeamEdit(
    password: Option[String],
    description: String,
    descPrivate: Option[String],
    request: Boolean,
    chat: Team.Access,
    forum: Team.Access,
    hideMembers: Boolean
) {

  def isOpen = !request

  def trim =
    copy(
      description = description,
      descPrivate = descPrivate.filter(_.nonEmpty)
    )
}

private[team] case class RequestSetup(
    message: Option[String],
    password: Option[String]
)
