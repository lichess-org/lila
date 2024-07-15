package lila.msg

import reactivemongo.api.bson.*
import scalalib.ThreadLocalRandom

import lila.db.BSON
import lila.db.dsl.{ *, given }

private object BsonHandlers:

  import Msg.Last
  given BSONDocumentHandler[Last] = Macros.handler

  given threadHandler: BSON[MsgThread] with
    def reads(r: BSON.Reader) =
      r.strsD("users") match
        case List(u1, u2) =>
          MsgThread(
            id = r.get[MsgThread.Id]("_id"),
            user1 = UserId(u1),
            user2 = UserId(u2),
            lastMsg = r.get[Last]("lastMsg"),
            maskFor = r.getO[UserId]("maskFor"),
            maskWith = r.getO[Last]("maskWith")
          )
        case x => sys.error(s"Invalid MsgThread users: $x")
    def writes(w: BSON.Writer, t: MsgThread) =
      $doc(
        "_id"      -> t.id,
        "users"    -> t.users.sorted(using stringOrdering),
        "lastMsg"  -> t.lastMsg,
        "maskFor"  -> t.maskFor,
        "maskWith" -> t.maskWith
      )

  given msgHandler: BSONDocumentHandler[Msg] = Macros.handler

  def writeMsg(msg: Msg, threadId: MsgThread.Id): Bdoc =
    msgHandler.writeTry(msg).get ++ $doc(
      "_id" -> ThreadLocalRandom.nextString(10),
      "tid" -> threadId
    )

  def writeThread(thread: MsgThread, delBy: List[UserId]): Bdoc =
    threadHandler.writeTry(thread).get ++ $doc("del" -> delBy)
      ++ $doc("maskWith" -> $doc("date" -> thread.lastMsg.date))
  // looks weird, but maybe.. it is the way

  def selectNotDeleted(using me: Me) =
    if UserId.lichess.is(me) then $empty // using "del" is too expensive
    else $doc("del".$ne(me.userId))
