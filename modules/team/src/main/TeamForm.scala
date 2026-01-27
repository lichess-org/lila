package lila.team

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{
  cleanNonEmptyText,
  cleanText,
  cleanTextWithSymbols,
  into,
  mustNotContainLichess,
  numberIn,
  given
}
import lila.core.captcha.CaptchaApi
import lila.core.team.Access
import lila.core.user.FlairApi
import lila.db.dsl.{ *, given }

final private class TeamForm(teamRepo: TeamRepo, captcha: CaptchaApi, flairApi: FlairApi)(using
    Executor
):
  import TeamForm.Fields

  def create(using Me) = Form:
    mapping(
      Fields.name,
      Fields.password,
      Fields.intro,
      Fields.description,
      Fields.descPrivate,
      Fields.request,
      "flair" -> flairApi.formField(),
      Fields.gameId,
      Fields.move
    )(TeamSetup.apply)(unapply)
      .verifying("team:teamAlreadyExists", d => !teamExists(d).await(2.seconds, "teamExists"))
      .verifying(lila.core.captcha.failMessage, captcha.validateSync)

  def edit(team: Team)(using Me) = Form(
    mapping(
      Fields.password,
      Fields.intro,
      Fields.description,
      Fields.descPrivate,
      Fields.request,
      Fields.chat,
      Fields.forum,
      Fields.hideMembers,
      "flair" -> flairApi.formField()
    )(TeamEdit.apply)(unapply)
  ).fill:
    TeamEdit(
      password = team.password,
      intro = team.intro,
      description = team.description,
      descPrivate = team.descPrivate,
      request = !team.open,
      chat = team.chat,
      forum = team.forum,
      hideMembers = team.hideMembers.has(true),
      flair = team.flair
    )

  def request(team: Team) = Form(
    mapping(
      Fields.requestMessage(team),
      Fields.passwordCheck(team)
    )(RequestSetup.apply)(unapply)
  ).fill(
    RequestSetup(
      message = TeamRequest.defaultMessage.some,
      password = None
    )
  )

  def apiRequest(team: Team) = Form:
    mapping(
      Fields.requestMessage(team),
      Fields.passwordCheck(team)
    )(RequestSetup.apply)(unapply)

  val processRequest = Form:
    tuple(
      "process" -> nonEmptyText,
      "url" -> nonEmptyText
    )

  val selectMember = Form:
    single:
      "userId" -> lila.common.Form.username.historicalField

  def createWithCaptcha(using Me) = create -> captcha.any

  val pmAll = Form:
    single("message" -> cleanTextWithSymbols(minLength = 3, maxLength = 9000))

  val explain = Form:
    single("explain" -> cleanText(minLength = 3, maxLength = 9000))

  def members = Form:
    single("members" -> nonEmptyText)

  val blocklist = Form:
    val sep = "\n"
    single:
      "names" -> cleanText(maxLength = 9000)
        .transform[String](_.split(sep).take(300).toList.flatMap(UserStr.read).mkString(sep), identity)

  val searchDeclinedForm: Form[Option[UserStr]] = Form(
    single("search" -> optional(lila.common.Form.username.historicalField))
  )

  val subscribe = Form(single("subscribe" -> optional(boolean)))

  private def teamExists(setup: TeamSetup) =
    teamRepo.coll.exists($id(Team.nameToId(setup.name)))

private case class TeamSetup(
    name: String,
    password: Option[String],
    intro: Option[String],
    description: Markdown,
    descPrivate: Option[Markdown],
    request: Boolean,
    flair: Option[Flair],
    gameId: GameId,
    move: String
) extends lila.core.captcha.WithCaptcha:
  def isOpen = !request

private case class TeamEdit(
    password: Option[String],
    intro: Option[String],
    description: Markdown,
    descPrivate: Option[Markdown],
    request: Boolean,
    chat: Access,
    forum: Access,
    hideMembers: Boolean,
    flair: Option[Flair]
):
  def isOpen = !request

  def trim =
    copy(
      description = description,
      descPrivate = descPrivate.filter(_.value.nonEmpty)
    )

private case class RequestSetup(
    message: Option[String],
    password: Option[String]
)

object TeamForm:
  object Fields:
    def name(using me: Me) =
      "name" -> cleanText(minLength = 3, maxLength = 60).verifying(mustNotContainLichess(me.isVerified))
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
    val request = "request" -> boolean
    val gameId = "gameId" -> of[GameId]
    val move = "move" -> text
    private def inAccess(cs: List[Access]) = numberIn(cs.map(_.id)).transform[Access](Access.byId, _.id)
    val chat = "chat" -> inAccess(Access.allInTeam)
    val forum = "forum" -> inAccess(Access.all)
    val hideMembers = "hideMembers" -> boolean

object TeamSingleChange:

  type Change[A] = lila.common.Form.SingleChange.Change[Team, A]
  private def changing[A] = lila.common.Form.SingleChange.changing[Team, TeamForm.Fields.type, A]

  val changes: Map[String, Change[?]] = List[Change[?]](
    changing(_.password): v =>
      _.copy(password = v),
    changing(_.intro): v =>
      _.copy(intro = v),
    changing(_.description): v =>
      _.copy(description = v),
    changing(_.descPrivate): v =>
      _.copy(descPrivate = v),
    changing(_.request): v =>
      _.copy(open = !v),
    changing(_.chat): v =>
      _.copy(chat = v),
    changing(_.forum): v =>
      _.copy(forum = v)
  ).map: change =>
    change.field -> change
  .toMap
