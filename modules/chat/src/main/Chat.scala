package lila.chat

import lila.user.User

sealed trait AnyChat {
  def id: ChatId
  def lines: List[Line]

  def forUser(u: Option[User]): AnyChat
}

sealed trait Chat[L <: Line] extends AnyChat {
  def id: ChatId
  def lines: List[L]
  def nonEmpty = lines exists (_.isHuman)
}

case class UserChat(
    id: ChatId,
    lines: List[UserLine]) extends Chat[UserLine] {

  def forUser(u: Option[User]) = u.??(_.troll).fold(this,
    copy(lines = lines filterNot (_.troll)))

  def markDeleted(u: User) = copy(
    lines = lines.map { l =>
      if (l.userId == u.id) l.delete else l
    })

  def add(line: UserLine) = copy(lines = lines :+ line)
}

case class MixedChat(
    id: ChatId,
    lines: List[Line]) extends Chat[Line] {

  def forUser(u: Option[User]) = u.??(_.troll).fold(this,
    copy(lines = lines filter {
      case l: UserLine   => !l.troll
      case l: PlayerLine => true
    }))
}

object Chat {

  import lila.db.BSON

  def makeUser(id: ChatId) = UserChat(id, Nil)
  def makeMixed(id: ChatId) = MixedChat(id, Nil)

  object BSONFields {
    val id = "_id"
    val lines = "l"
    val encoded = "e"
  }

  import BSONFields._
  import reactivemongo.bson.BSONDocument

  implicit val mixedChatBSONHandler = new BSON[MixedChat] {
    def reads(r: BSON.Reader): MixedChat = {
      implicit val lineReader = Line.lineBSONHandler(r.boolO(encoded) | true)
      MixedChat(
        id = r str id,
        lines = r.get[List[Line]](lines))
    }
    def writes(w: BSON.Writer, o: MixedChat) = BSONDocument(
      id -> o.id,
      lines -> o.lines.map(Line.lineBSONHandler(false).write),
      encoded -> false)
  }

  implicit val userChatBSONHandler = new BSON[UserChat] {
    def reads(r: BSON.Reader): UserChat = {
      implicit val lineReader = Line.userLineBSONHandler(r.boolO(encoded) | true)
      UserChat(
        id = r str id,
        lines = r.get[List[UserLine]](lines))
    }
    def writes(w: BSON.Writer, o: UserChat) = BSONDocument(
      id -> o.id,
      lines -> o.lines.map(Line.lineBSONHandler(false).write),
      encoded -> false)
  }
}
