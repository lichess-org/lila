package lila.msg

import reactivemongo.api.bson.*

import lila.user.User
import lila.db.dsl.{ *, given }
import lila.db.BSON

private object BsonHandlers:

  import Msg.Last
  given BSONDocumentHandler[Last] = Macros.handler

  given BSONHandler[MsgThread.Id] = stringAnyValHandler[MsgThread.Id](_.value, MsgThread.Id.apply)

  given threadHandler: BSON[MsgThread] with
    def reads(r: BSON.Reader) =
      r.strsD("users") match
        case List(u1, u2) =>
          MsgThread(
            id = r.get[MsgThread.Id]("_id"),
            user1 = u1,
            user2 = u2,
            lastMsg = r.get[Last]("lastMsg")
          )
        case x => sys error s"Invalid MsgThread users: $x"
    def writes(w: BSON.Writer, t: MsgThread) =
      $doc(
        "_id"     -> t.id,
        "users"   -> t.users.sorted,
        "lastMsg" -> t.lastMsg
      )

  // given BSONHandler[Msg.Id]                  = stringAnyValHandler[Msg.Id](_.value, Msg.Id.apply)
  given msgHandler: BSONDocumentHandler[Msg] = Macros.handler

  def writeMsg(msg: Msg, threadId: MsgThread.Id): Bdoc =
    msgHandler.writeTry(msg).get ++ $doc(
      "_id" -> lila.common.ThreadLocalRandom.nextString(10),
      "tid" -> threadId
    )

  def writeThread(thread: MsgThread, delBy: List[User.ID]): Bdoc =
    threadHandler.writeTry(thread).get ++ $doc("del" -> delBy)
