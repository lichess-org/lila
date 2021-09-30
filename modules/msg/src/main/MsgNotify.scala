package lila.msg

import akka.actor.Cancellable
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._

import lila.db.dsl._
import lila.notify.{ Notification, PrivateMessage }
import lila.common.String.shorten
import lila.user.User

final private class MsgNotify(
    colls: MsgColls,
    notifyApi: lila.notify.NotifyApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler
) {

  import BsonHandlers._

  private val delay = 5 seconds

  private val delayed = new ConcurrentHashMap[MsgThread.Id, Cancellable](256)

  def onPost(threadId: MsgThread.Id): Unit = schedule(threadId)

  def onRead(threadId: MsgThread.Id, userId: User.ID, contactId: User.ID): Funit = {
    !cancel(threadId) ??
      notifyApi
        .markRead(
          lila.notify.Notification.Notifies(userId),
          $doc(
            "content.type" -> "privateMessage",
            "content.user" -> contactId
          )
        )
        .void
  }

  def deleteAllBy(threads: List[MsgThread], user: User): Funit =
    threads
      .map { thread =>
        cancel(thread.id)
        notifyApi
          .remove(
            lila.notify.Notification.Notifies(thread other user),
            $doc("content.user" -> user.id)
          )
          .void
      }
      .sequenceFu
      .void

  private def schedule(threadId: MsgThread.Id): Unit =
    delayed
      .compute(
        threadId,
        (id, canc) => {
          Option(canc).foreach(_.cancel())
          scheduler.scheduleOnce(delay) {
            delayed remove id
            doNotify(threadId).unit
          }
        }
      )
      .unit

  private def cancel(threadId: MsgThread.Id): Boolean =
    Option(delayed remove threadId).map(_.cancel()).isDefined

  private def doNotify(threadId: MsgThread.Id): Funit =
    colls.thread.byId[MsgThread](threadId.value) flatMap {
      _ ?? { thread =>
        val msg  = thread.lastMsg
        val dest = thread other msg.user
        !thread.delBy(dest) ?? {
          lila.common.Bus.publish(MsgThread.Unread(thread), "msgUnread")
          notifyApi addNotification Notification.make(
            Notification.Notifies(dest),
            PrivateMessage(
              PrivateMessage.Sender(msg.user),
              PrivateMessage.Text(shorten(msg.text, 40))
            )
          )
        }
      }
    }
}
