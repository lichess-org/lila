package lila.msg

import reactivemongo.api._
import play.api.data._
import play.api.data.Forms._

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

  val postForm = Form(single("text" -> nonEmptyText(maxLength = 10_000)))

  def post(me: User, other: User, text: String): Fu[Msg] = {
    val msg = Msg.make(me.id, other.id, text)
    colls.msg.insert.one(msg) zip
      colls.thread.updateField($id(msg.thread), "lastMsg", msg.asLast) inject
      msg
  }

  private def withMsgs(thread: MsgThread): Fu[MsgThread.WithMsgs] =
    colls.msg.ext
      .find($doc("thread" -> thread.id))
      .sort($sort desc "date")
      .list[Msg](100)
      .map { MsgThread.WithMsgs(thread, _) }
}
