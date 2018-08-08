package lidraughts.message

import lidraughts.db.dsl._
import lidraughts.user.User

final class MessageBatch(
    coll: Coll,
    notifyApi: lidraughts.notify.NotifyApi
) {

  def apply(me: User, action: String, ids: List[String]): Funit = ids.nonEmpty ?? {
    action match {
      case "read" => markRead(me, ids)
      case "unread" => markUnread(me, ids)
      case "delete" => delete(me, ids)
      case x => fufail(s"Invalid message batch action: $x")
    }
  }

  def markRead(me: User, ids: List[String]): Funit =
    ThreadRepo.visibleByUserByIds(me, ids).flatMap {
      _.map(ThreadRepo.setReadFor(me)).sequenceFu
    }.void

  def markUnread(me: User, ids: List[String]): Funit =
    ThreadRepo.visibleByUserByIds(me, ids).flatMap {
      _.map(ThreadRepo.setUnreadFor(me)).sequenceFu
    }.void

  def delete(me: User, ids: List[String]): Funit =
    ThreadRepo.visibleByUserByIds(me, ids).flatMap {
      _.map { thread =>
        ThreadRepo.deleteFor(me.id)(thread.id) zip
          notifyApi.remove(
            lidraughts.notify.Notification.Notifies(me.id),
            $doc("content.thread.id" -> thread.id)
          ) void
      }.sequenceFu.void
    }
}
