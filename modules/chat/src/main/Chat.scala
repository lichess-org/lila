package lila.chat

import lila.user.User

sealed trait AnyChat {
  def id: ChatId
  def lines: List[Line]

  val loginRequired: Boolean

  def forUser(u: Option[User]): AnyChat

  def isEmpty = lines.isEmpty

  def userIds: List[User.ID]
}

sealed trait Chat[L <: Line] extends AnyChat {
  def id: ChatId
  def lines: List[L]
  def nonEmpty = lines.exists(_.isHuman)
}

case class UserChat(
    id: ChatId,
    lines: List[UserLine]
) extends Chat[UserLine] {

  val loginRequired = true

  def forUser(u: Option[User]) = u.??(_.troll).fold(
    this,
    copy(lines = lines filterNot (_.troll))
  )

  def markDeleted(u: User) = copy(
    lines = lines.map { l =>
      if (l.userId == u.id) l.delete else l
    }
  )

  def add(line: UserLine) = copy(lines = lines :+ line)

  def mapLines(f: UserLine => UserLine) = copy(lines = lines map f)

  def userIds = lines.map(_.userId)

  def truncate(max: Int) = copy(lines = lines.drop((lines.size - max) atLeast 0))
}

object UserChat {
  case class Mine(chat: UserChat, timeout: Boolean) {
    def truncate(max: Int) = copy(chat = chat truncate max)
  }
}

case class MixedChat(
    id: ChatId,
    lines: List[Line]
) extends Chat[Line] {

  val loginRequired = false

  def forUser(u: Option[User]) = u.??(_.troll).fold(
    this,
    copy(lines = lines filter {
      case l: UserLine => !l.troll
      case l: PlayerLine => true
    })
  )

  def mapLines(f: Line => Line) = copy(lines = lines map f)

  def userIds = lines.collect {
    case l: UserLine => l.userId
  }
}

object Chat {

  // if restricted, only presets are available
  case class Restricted(chat: MixedChat, restricted: Boolean)

  // left: game chat
  // right: tournament/simul chat
  case class GameOrEvent(either: Either[Restricted, UserChat.Mine]) {
    def game = either.left.toOption
  }

  import lila.db.BSON

  def makeUser(id: ChatId) = UserChat(id, Nil)
  def makeMixed(id: ChatId) = MixedChat(id, Nil)

  object BSONFields {
    val id = "_id"
    val lines = "l"
  }

  import BSONFields._
  import reactivemongo.bson.BSONDocument
  import Line.{ lineBSONHandler, userLineBSONHandler }

  implicit val mixedChatBSONHandler = new BSON[MixedChat] {
    def reads(r: BSON.Reader): MixedChat = {
      MixedChat(
        id = r str id,
        lines = r.get[List[Line]](lines)
      )
    }
    def writes(w: BSON.Writer, o: MixedChat) = BSONDocument(
      id -> o.id,
      lines -> o.lines
    )
  }

  implicit val userChatBSONHandler = new BSON[UserChat] {
    def reads(r: BSON.Reader): UserChat = {
      UserChat(
        id = r str id,
        lines = r.get[List[UserLine]](lines)
      )
    }
    def writes(w: BSON.Writer, o: UserChat) = BSONDocument(
      id -> o.id,
      lines -> o.lines
    )
  }
}
