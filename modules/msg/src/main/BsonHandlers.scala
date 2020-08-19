package lila.msg

import reactivemongo.api.bson._

import lila.user.User
import lila.db.dsl._
import lila.db.BSON

private object BsonHandlers {

  import Msg.Last
  implicit val msgContentHandler = Macros.handler[Last]

  implicit val threadIdHandler = stringAnyValHandler[MsgThread.Id](_.value, MsgThread.Id.apply)

  implicit val threadHandler = new BSON[MsgThread] {
    def reads(r: BSON.Reader) =
      r.strsD("users") match {
        case List(u1, u2) =>
          MsgThread(
            id = r.get[MsgThread.Id]("_id"),
            user1 = u1,
            user2 = u2,
            lastMsg = r.get[Last]("lastMsg")
          )
        case x => sys error s"Invalid MsgThread users: $x"
      }
    def writes(w: BSON.Writer, t: MsgThread) =
      $doc(
        "_id"     -> t.id,
        "users"   -> t.users.sorted,
        "lastMsg" -> t.lastMsg
      )
  }

  implicit val msgIdHandler = stringAnyValHandler[Msg.Id](_.value, Msg.Id.apply)
  implicit val msgHandler   = Macros.handler[Msg]

  def writeMsg(msg: Msg, threadId: MsgThread.Id): Bdoc =
    msgHandler.writeTry(msg).get ++ $doc(
      "_id" -> lila.common.ThreadLocalRandom.nextString(10),
      "tid" -> threadId
    )

  def writeThread(thread: MsgThread, delBy: List[User.ID]): Bdoc =
    threadHandler.writeTry(thread).get ++ $doc("del" -> delBy)
}
