package lila.msg

import reactivemongo.api._

import lila.db.dsl._
import lila.user.User

final class MsgApi(
    colls: MsgColls
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def threads(me: User): Fu[List[MsgThread]] =
    colls.thread.ext
      .find(
        $doc(
          "users" -> me.id,
          "blockers" $ne me.id
        )
      )
      .sort($sort desc "lastMsg.date")
      .list[MsgThread](100)

  def convoWith(me: User, other: User): Fu[MsgThread.WithMsgs] =
    colls.thread.ext
      .find(
        $doc("_id" -> MsgThread.id(me.id, other.id))
      )
      .one[MsgThread]
      .dmap { _ | MsgThread.make(me.id, other.id) }
      .flatMap(withMsgs)

  private def withMsgs(thread: MsgThread): Fu[MsgThread.WithMsgs] =
    colls.msg.ext
      .find($doc("thread" -> thread.id))
      .sort($sort desc "date")
      .list[Msg](100)
      .dmap { MsgThread.WithMsgs(thread, _) }
}
