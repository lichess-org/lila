package lila.msg

import lila.db.dsl._
import lila.db.BSON
import reactivemongo.api.bson._

private[msg] object BsonHandlers {

  import Msg.Last
  implicit val msgContentBSONHandler = Macros.handler[Last]

  implicit val threadIdBSONHandler = stringAnyValHandler[MsgThread.Id](_.value, MsgThread.Id.apply)

  implicit val threadBSONHandler = new BSON[MsgThread] {
    def reads(r: BSON.Reader) = r.strsD("users") match {
      case List(u1, u2) =>
        MsgThread(
          id = r.get[MsgThread.Id]("_id"),
          user1 = u1,
          user2 = u2,
          lastMsg = r.get[Last]("lastMsg")
        )
      case x => sys error s"Invalid MsgThread users: $x"
    }
    def writes(w: BSON.Writer, t: MsgThread) = $doc(
      "_id"     -> t.id,
      "users"   -> t.users.sorted,
      "lastMsg" -> t.lastMsg
    )
  }

  implicit val msgIdBSONHandler = stringAnyValHandler[Msg.Id](_.value, Msg.Id.apply)
  implicit val msgBSONHandler   = Macros.handler[Msg]
}
