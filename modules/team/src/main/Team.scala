package lila.team

import org.joda.time.DateTime
import java.security.MessageDigest
import java.nio.charset.StandardCharsets.UTF_8
import scala.util.chaining._

import lila.user.User
import org.joda.time.Days
import lila.common.Markdown

case class Team(
    _id: Team.ID, // also the url slug
    name: String,
    password: Option[String],
    description: Markdown,
    descPrivate: Option[Markdown],
    nbMembers: Int,
    enabled: Boolean,
    open: Boolean,
    createdAt: DateTime,
    createdBy: User.ID,
    leaders: Set[User.ID],
    chat: Team.Access,
    forum: Team.Access,
    hideMembers: Option[Boolean]
) {

  def id = _id

  def slug = id

  def disabled = !enabled

  def isChatFor(f: Team.Access.type => Team.Access) =
    chat == f(Team.Access)

  def isForumFor(f: Team.Access.type => Team.Access) =
    forum == f(Team.Access)

  def publicMembers: Boolean = !hideMembers.has(true)

  def passwordMatches(pw: String) =
    password.forall(teamPw => MessageDigest.isEqual(teamPw.getBytes(UTF_8), pw.getBytes(UTF_8)))

  def isOnlyLeader(userId: User.ID) = leaders == Set(userId)
}

object Team {

  type ID = String

  case class Mini(id: Team.ID, name: String)

  val variants: Map[chess.variant.Variant, Mini] = chess.variant.Variant.all.view collect {
    case v if v.exotic =>
      val name = s"Lichess ${v.name}"
      v -> Mini(nameToId(name), name)
  } toMap

  val maxJoinCeiling = 50

  def maxJoin(u: User) =
    if (u.isVerified) maxJoinCeiling * 2
    else {
      15 + Days.daysBetween(u.createdAt, DateTime.now).getDays / 7
    } atMost maxJoinCeiling

  type Access = Int
  object Access {
    val NONE      = 0
    val LEADERS   = 10
    val MEMBERS   = 20
    val EVERYONE  = 30
    val allInTeam = List(NONE, LEADERS, MEMBERS)
    val all       = EVERYONE :: allInTeam
  }

  case class IdsStr(value: String) extends AnyVal {

    import IdsStr.separator

    def contains(teamId: ID) =
      value == teamId ||
        value.startsWith(s"$teamId$separator") ||
        value.endsWith(s"$separator$teamId") ||
        value.contains(s"$separator$teamId$separator")

    def toArray: Array[String] = value split IdsStr.separator
    def toList                 = value.nonEmpty ?? toArray.toList
    def toSet                  = value.nonEmpty ?? toArray.toSet
  }

  object IdsStr {

    private val separator = ' '

    val empty = IdsStr("")

    def apply(ids: Iterable[ID]): IdsStr = IdsStr(ids mkString separator.toString)
  }

  def make(
      id: String,
      name: String,
      password: Option[String],
      description: Markdown,
      descPrivate: Option[Markdown],
      open: Boolean,
      createdBy: User
  ): Team =
    new Team(
      _id = id,
      name = name,
      password = password,
      description = description,
      descPrivate = descPrivate,
      nbMembers = 1,
      enabled = true,
      open = open,
      createdAt = DateTime.now,
      createdBy = createdBy.id,
      leaders = Set(createdBy.id),
      chat = Access.MEMBERS,
      forum = Access.MEMBERS,
      hideMembers = none
    )

  def nameToId(name: String) =
    (lila.common.String slugify name) pipe { slug =>
      // if most chars are not latin, go for random slug
      if (slug.lengthIs > (name.length / 2)) slug else randomId()
    }

  private[team] def randomId() = lila.common.ThreadLocalRandom nextString 8
}
