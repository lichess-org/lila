package lila.chat

sealed trait AnyChat {
  def id: ChatId
  def lines: List[Line]

  def toJsonString = Line toJsonString lines
}

sealed trait Chat[L <: Line] extends AnyChat {
  def id: ChatId
  def lines: List[L]
}

case class UserChat(
    id: ChatId,
    lines: List[UserLine]) extends Chat[UserLine] {
}

case class MixedChat(
    id: ChatId,
    lines: List[Line]) extends Chat[Line] {
}

object Chat {

  import lila.db.BSON

  def makeUser(id: ChatId) = UserChat(id, Nil)
  def makeMixed(id: ChatId) = MixedChat(id, Nil)

  object BSONFields {
    val id = "_id"
    val lines = "l"
  }

  import BSONFields._
  import reactivemongo.bson.BSONDocument

  implicit val mixedChatBSONHandler = new BSON[MixedChat] {
    implicit def lineHandler = Line.lineBSONHandler
    def reads(r: BSON.Reader): MixedChat = MixedChat(id = r str id, lines = r.get[List[Line]](lines))
    def writes(w: BSON.Writer, o: MixedChat) = BSONDocument(id -> o.id, lines -> o.lines)
  }

  implicit val userChatBSONHandler = new BSON[UserChat] {
    def reads(r: BSON.Reader): UserChat = UserChat(id = r str id, lines = r.get[List[UserLine]](lines))
    def writes(w: BSON.Writer, o: UserChat) = BSONDocument(id -> o.id, lines -> o.lines)
  }
}
