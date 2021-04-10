package lila.chat

import lila.hub.actorApi.shutup.PublicSource
import lila.user.User

sealed trait AnyChat {
  def id: Chat.Id
  def lines: List[Line]

  val loginRequired: Boolean

  def forUser(u: Option[User]): AnyChat

  def isEmpty = lines.isEmpty

  def userIds: List[User.ID]
}

sealed trait Chat[L <: Line] extends AnyChat {
  def id: Chat.Id
  def lines: List[L]
  def nonEmpty = lines.exists(_.isHuman)
}

case class UserChat(
    id: Chat.Id,
    lines: List[UserLine]
) extends Chat[UserLine] {

  val loginRequired = true

  def forUser(u: Option[User]): UserChat =
    if (u.??(_.marks.troll)) this
    else copy(lines = lines filterNot (_.troll))

  def markDeleted(u: User) =
    copy(
      lines = lines.map { l =>
        if (l.userId == u.id) l.delete else l
      }
    )

  def hasLinesOf(u: User) = lines.exists(_.userId == u.id)

  def add(line: UserLine) = copy(lines = lines :+ line)

  def mapLines(f: UserLine => UserLine) = copy(lines = lines map f)

  def userIds = lines.map(_.userId)

  def truncate(max: Int) = copy(lines = lines.drop((lines.size - max) atLeast 0))

  def hasRecentLine(u: User): Boolean = lines.reverse.take(12).exists(_.userId == u.id)
}

object UserChat {
  case class Mine(chat: UserChat, timeout: Boolean) {
    def truncate(max: Int) = copy(chat = chat truncate max)
  }
}

case class MixedChat(
    id: Chat.Id,
    lines: List[Line]
) extends Chat[Line] {

  val loginRequired = false

  def forUser(u: Option[User]): MixedChat =
    if (u.??(_.marks.troll)) this
    else
      copy(lines = lines filter {
        case l: UserLine   => !l.troll
        case _: PlayerLine => true
      })

  def mapLines(f: Line => Line) = copy(lines = lines map f)

  def userIds =
    lines.collect { case l: UserLine =>
      l.userId
    }
}

object Chat {

  case class Id(value: String) extends AnyVal with StringValue

  case class ResourceId(value: String) extends AnyVal with StringValue

  case class Setup(id: Id, publicSource: PublicSource)

  def tournamentSetup(tourId: String) = Setup(Id(tourId), PublicSource.Tournament(tourId))
  def simulSetup(simulId: String)     = Setup(Id(simulId), PublicSource.Simul(simulId))

  // if restricted, only presets are available
  case class Restricted(chat: MixedChat, restricted: Boolean)

  // left: game chat
  // right: tournament/simul chat
  case class GameOrEvent(either: Either[Restricted, (UserChat.Mine, ResourceId)]) {
    def game = either.left.toOption
  }

  import lila.db.BSON

  def makeUser(id: Chat.Id)  = UserChat(id, Nil)
  def makeMixed(id: Chat.Id) = MixedChat(id, Nil)

  def chanOf(id: Chat.Id) = s"chat:$id"

  object BSONFields {
    val id    = "_id"
    val lines = "l"
  }

  import BSONFields._
  import reactivemongo.api.bson.BSONDocument
  import Line.{ lineBSONHandler, userLineBSONHandler }

  implicit val chatIdIso         = lila.common.Iso.string[Id](Id.apply, _.value)
  implicit val chatIdBSONHandler = lila.db.BSON.stringIsoHandler(chatIdIso)

  implicit val mixedChatBSONHandler = new BSON[MixedChat] {
    def reads(r: BSON.Reader): MixedChat = {
      MixedChat(
        id = r.get[Id](id),
        lines = r.get[List[Line]](lines)
      )
    }
    def writes(w: BSON.Writer, o: MixedChat) =
      BSONDocument(
        id    -> o.id,
        lines -> o.lines
      )
  }

  implicit val userChatBSONHandler = new BSON[UserChat] {
    def reads(r: BSON.Reader): UserChat = {
      UserChat(
        id = r.get[Id](id),
        lines = r.get[List[UserLine]](lines)
      )
    }
    def writes(w: BSON.Writer, o: UserChat) =
      BSONDocument(
        id    -> o.id,
        lines -> o.lines
      )
  }
}
