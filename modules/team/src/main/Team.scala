package lila.team

import reactivemongo.api.bson.Macros.Annotations.Key
import java.security.MessageDigest
import java.nio.charset.StandardCharsets.UTF_8
import scala.util.chaining.*
import ornicar.scalalib.ThreadLocalRandom

import lila.user.User
import lila.hub.LightTeam

case class Team(
    @Key("_id") id: TeamId, // also the url slug
    name: String,
    password: Option[String],
    intro: Option[String],
    description: Markdown,
    descPrivate: Option[Markdown],
    nbMembers: Int,
    enabled: Boolean,
    open: Boolean,
    createdAt: Instant,
    createdBy: UserId,
    chat: Team.Access,
    forum: Team.Access,
    hideMembers: Option[Boolean],
    flair: Option[Flair]
):

  inline def slug     = id
  inline def disabled = !enabled

  def isChatFor(f: Team.Access.type => Team.Access) =
    chat == f(Team.Access)

  def isForumFor(f: Team.Access.type => Team.Access) =
    forum == f(Team.Access)

  def publicMembers: Boolean = !hideMembers.has(true)

  def passwordMatches(pw: String) =
    password.forall(teamPw => MessageDigest.isEqual(teamPw.getBytes(UTF_8), pw.getBytes(UTF_8)))

  def light = LightTeam(id, name, flair)

object Team:

  case class WithLeaders(team: Team, leaders: List[TeamMember]):
    export team.*
    def hasAdminCreator = leaders.exists(l => l.is(team.createdBy) && l.hasPerm(_.Admin))
    def publicLeaders   = leaders.filter(_.hasPerm(_.Public))

  case class IdAndLeaderIds(id: TeamId, leaderIds: Set[UserId])

  case class WithMyLeadership(team: Team, amLeader: Boolean):
    export team.*

  case class WithPublicLeaderIds(team: Team, publicLeaders: List[UserId])

  import chess.variant.Variant
  val variants: Map[Variant.LilaKey, LightTeam] = Variant.list.all.view.collect {
    case v if v.exotic =>
      val name = s"Lichess ${v.name}"
      v.key -> LightTeam(nameToId(name), name, none)
  }.toMap

  val maxLeaders     = 10
  val maxJoinCeiling = 50

  def maxJoin(u: User) =
    if u.isVerified then maxJoinCeiling * 2
    else
      {
        15 + daysBetween(u.createdAt, nowInstant) / 7
      } atMost maxJoinCeiling

  type Access = Int
  object Access:
    val NONE      = 0
    val LEADERS   = 10
    val MEMBERS   = 20
    val EVERYONE  = 30
    val allInTeam = List(NONE, LEADERS, MEMBERS)
    val all       = EVERYONE :: allInTeam

  case class IdsStr(value: String) extends AnyVal:

    import IdsStr.separator

    def contains(teamId: TeamId) =
      value == teamId.value ||
        value.startsWith(s"$teamId$separator") ||
        value.endsWith(s"$separator$teamId") ||
        value.contains(s"$separator$teamId$separator")

    def toArray: Array[TeamId] = TeamId.from(value split IdsStr.separator)
    def toList                 = value.nonEmpty so toArray.toList
    def toSet                  = value.nonEmpty so toArray.toSet
    def size                   = value.count(_ == separator) + 1
    def nonEmpty               = value.nonEmpty

  object IdsStr:

    private val separator = ' '

    val empty = IdsStr("")

    def apply(ids: Iterable[TeamId]): IdsStr = IdsStr(ids mkString separator.toString)

  def make(
      id: TeamId,
      name: LightTeam.TeamName,
      password: Option[String],
      intro: Option[String],
      description: Markdown,
      descPrivate: Option[Markdown],
      open: Boolean,
      createdBy: User
  ): Team = new Team(
    id = id,
    name = name,
    password = password,
    intro = intro,
    description = description,
    descPrivate = descPrivate,
    nbMembers = 1,
    enabled = true,
    open = open,
    createdAt = nowInstant,
    createdBy = createdBy.id,
    chat = Access.MEMBERS,
    forum = Access.MEMBERS,
    hideMembers = none,
    flair = none
  )

  def nameToId(name: String) =
    val slug = lila.common.String.slugify(name)
    // if most chars are not latin, go for random slug
    if slug.lengthIs > (name.length / 2) then TeamId(slug) else randomId()

  private[team] def randomId() = TeamId(ThreadLocalRandom nextString 8)
