package lila.team

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.Constraints

import lila.common.Form.{
  cleanNonEmptyText,
  cleanText,
  cleanTextWithSymbols,
  mustNotContainLichess,
  numberIn,
  into,
  given
}
import lila.db.dsl.{ *, given }

final private[team] class TeamForm(
    teamRepo: TeamRepo,
    lightUserApi: lila.user.LightUserApi,
    val captcher: lila.hub.actors.Captcher
)(using Executor)
    extends lila.hub.CaptchedForm:

  private object Fields:
    val name = "name" -> cleanText(minLength = 3, maxLength = 60).verifying(mustNotContainLichess(false))
    val password = "password" -> optional(cleanText(maxLength = 60))
    def passwordCheck(team: Team) = "password" -> optional(text).verifying(
      "team:incorrectEntryCode",
      pw => team.passwordMatches(pw.so(_.trim))
    )
    def requestMessage(team: Team) =
      "message" -> optional(cleanText(minLength = 30, maxLength = 2000))
        .verifying("Request message required", msg => msg.isDefined || team.open)
    val intro =
      "intro" -> optional(cleanText(minLength = 3, maxLength = 200))
    val description =
      "description" -> cleanText(minLength = 30, maxLength = 4000).into[Markdown]
    val descPrivate =
      "descPrivate" -> optional(cleanNonEmptyText(maxLength = 4000).into[Markdown])
    val request     = "request"     -> boolean
    val gameId      = "gameId"      -> of[GameId]
    val move        = "move"        -> text
    val chat        = "chat"        -> numberIn(Team.Access.allInTeam)
    val forum       = "forum"       -> numberIn(Team.Access.all)
    val hideMembers = "hideMembers" -> boolean

  val create = Form:
    mapping(
      Fields.name,
      Fields.password,
      Fields.intro,
      Fields.description,
      Fields.descPrivate,
      Fields.request,
      Fields.gameId,
      Fields.move
    )(TeamSetup.apply)(unapply)
      .verifying("team:teamAlreadyExists", d => !teamExists(d).await(2 seconds, "teamExists"))
      .verifying(captchaFailMessage, validateCaptcha)

  def edit(team: Team) = Form(
    mapping(
      Fields.password,
      Fields.intro,
      Fields.description,
      Fields.descPrivate,
      Fields.request,
      Fields.chat,
      Fields.forum,
      Fields.hideMembers
    )(TeamEdit.apply)(unapply)
  ) fill TeamEdit(
    password = team.password,
    intro = team.intro,
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
    )(RequestSetup.apply)(unapply)
  ) fill RequestSetup(
    message = TeamRequest.defaultMessage.some,
    password = None
  )

  def apiRequest(team: Team) = Form:
    mapping(
      Fields.requestMessage(team),
      Fields.passwordCheck(team)
    )(RequestSetup.apply)(unapply)

  val processRequest = Form:
    tuple(
      "process" -> nonEmptyText,
      "url"     -> nonEmptyText
    )

  val selectMember = Form:
    single:
      "userId" -> lila.user.UserForm.historicalUsernameField

  def createWithCaptcha = withCaptcha(create)

  val pmAll = Form:
    single("message" -> cleanTextWithSymbols.verifying(Constraints minLength 3, Constraints maxLength 9000))

  val explain = Form:
    single("explain" -> cleanText(minLength = 3, maxLength = 9000))

  def members = Form:
    single("members" -> nonEmptyText)

  val blocklist = Form:
    val sep = "\n"
    single:
      "names" -> cleanText(maxLength = 9000)
        .transform[String](_.split(sep).take(300).toList.flatMap(UserStr.read).mkString(sep), identity)

  private def teamExists(setup: TeamSetup) =
    teamRepo.coll.exists($id(Team nameToId setup.name))

private[team] case class TeamSetup(
    name: String,
    password: Option[String],
    intro: Option[String],
    description: Markdown,
    descPrivate: Option[Markdown],
    request: Boolean,
    gameId: GameId,
    move: String
):
  def isOpen = !request

private[team] case class TeamEdit(
    password: Option[String],
    intro: Option[String],
    description: Markdown,
    descPrivate: Option[Markdown],
    request: Boolean,
    chat: Team.Access,
    forum: Team.Access,
    hideMembers: Boolean
):

  def isOpen = !request

  def trim =
    copy(
      description = description,
      descPrivate = descPrivate.filter(_.value.nonEmpty)
    )

private[team] case class RequestSetup(
    message: Option[String],
    password: Option[String]
)
