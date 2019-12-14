package lila.message

import lila.db.dsl._
import lila.user.User

final class MessageBatch(
    threadRepo: ThreadRepo,
    notifyApi: lila.notify.NotifyApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(me: User, action: String, ids: List[String]): Funit = ids.nonEmpty ?? {
    action match {
      case "read"   => markRead(me, ids)
      case "unread" => markUnread(me, ids)
      case "delete" => delete(me, ids)
      case x        => fufail(s"Invalid message batch action: $x")
    }
  }

  def markRead(me: User, ids: List[String]): Funit =
    threadRepo
      .visibleByUserByIds(me, ids)
      .flatMap {
        _.map(threadRepo.setReadFor(me)).sequenceFu
      }
      .void

  def markUnread(me: User, ids: List[String]): Funit =
    threadRepo
      .visibleByUserByIds(me, ids)
      .flatMap {
        _.map(threadRepo.setUnreadFor(me)).sequenceFu
      }
      .void

  def delete(me: User, ids: List[String]): Funit =
    threadRepo.visibleByUserByIds(me, ids).flatMap {
      _.map { thread =>
        threadRepo.deleteFor(me.id)(thread.id) zip
          notifyApi.remove(
            lila.notify.Notification.Notifies(me.id),
            $doc("content.thread.id" -> thread.id)
          ) void
      }.sequenceFu.void
    }
}
