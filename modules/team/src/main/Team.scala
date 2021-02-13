package lila.team

import org.joda.time.DateTime
import scala.util.chaining._

import lila.user.User

case class Team(
    _id: Team.ID, // also the url slug
    name: String,
    location: Option[String],
    description: String,
    nbMembers: Int,
    enabled: Boolean,
    open: Boolean,
    createdAt: DateTime,
    createdBy: User.ID,
    leaders: Set[User.ID],
    chat: Team.ChatFor
) {

  def id = _id

  def slug = id

  def disabled = !enabled

  def isChatFor(f: Team.ChatFor.type => Team.ChatFor) =
    chat == f(Team.ChatFor)
}

object Team {

  val maxJoin = 100

  type ID = String

  type ChatFor = Int
  object ChatFor {
    val NONE    = 0
    val LEADERS = 10
    val MEMBERS = 20
    val all     = List(NONE, LEADERS, MEMBERS)
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
  }

  object IdsStr {

    private val separator = ' '

    val empty = IdsStr("")

    def apply(ids: Iterable[ID]): IdsStr = IdsStr(ids mkString separator.toString)
  }

  def make(
      name: String,
      location: Option[String],
      description: String,
      open: Boolean,
      createdBy: User
  ): Team =
    new Team(
      _id = nameToId(name),
      name = name,
      location = location,
      description = description,
      nbMembers = 1,
      enabled = true,
      open = open,
      createdAt = DateTime.now,
      createdBy = createdBy.id,
      leaders = Set(createdBy.id),
      chat = ChatFor.MEMBERS
    )

  def nameToId(name: String) =
    (lila.common.String slugify name) pipe { slug =>
      // if most chars are not latin, go for random slug
      if (slug.lengthIs > (name.lengthIs / 2)) slug else lila.common.ThreadLocalRandom nextString 8
    }
}
