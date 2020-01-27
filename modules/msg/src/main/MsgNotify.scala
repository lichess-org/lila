package lila.msg

import akka.actor.Cancellable
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._

import lila.db.dsl._
import lila.notify.{ Notification, PrivateMessage }
import lila.common.String.shorten

final private class MsgNotify(
    colls: MsgColls,
    notifyApi: lila.notify.NotifyApi
)(implicit ec: scala.concurrent.ExecutionContext, scheduler: akka.actor.Scheduler) {

  import BsonHandlers._

  private val delay = 5 seconds

  private val delayed = new ConcurrentHashMap[MsgThread.Id, Cancellable](256)

  def onPost(threadId: MsgThread.Id): Unit = schedule(threadId)

  def onRead(threadId: MsgThread.Id): Unit = cancel(threadId)

  private def schedule(threadId: MsgThread.Id): Unit = delayed.compute(
    threadId,
    (id, canc) => {
      Option(canc).foreach(_.cancel)
      scheduler.scheduleOnce(delay) {
        delayed remove id
        doNotify(threadId)
      }
    }
  )

  private def cancel(threadId: MsgThread.Id): Unit =
    Option(delayed remove threadId).foreach(_.cancel)

  private def doNotify(threadId: MsgThread.Id): Funit =
    colls.thread.byId[MsgThread](threadId.value) flatMap {
      _ ?? { thread =>
        val msg = thread.lastMsg
        lila.common.Bus.publish(MsgThread.Unread(thread), "msgUnread")
        notifyApi addNotification Notification.make(
          Notification.Notifies(thread other msg.user),
          PrivateMessage(
            PrivateMessage.Sender(msg.user),
            PrivateMessage.Text(shorten(msg.text, 80))
          )
        )
      }
    }
}
