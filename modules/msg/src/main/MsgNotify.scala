package lila.msg

import lila.db.dsl._

final private class MsgNotify(
    colls: MsgColls,
    notifyApi: lila.notify.NotifyApi
)(implicit ec: scala.concurrent.ExecutionContext, scheduler: akka.actor.Scheduler) {

  import BsonHandlers._

  def onPost(msg: Msg): Funit =
    colls.thread.byId[MsgThread](msg.thread.value) flatMap {
      _ ?? { thread =>
        import lila.notify.{ Notification, PrivateMessage }
        import lila.common.String.shorten
        lila.common.Bus.publish(MsgThread.Unread(thread), "msgUnread")
        notifyApi addNotification Notification.make(
          Notification.Notifies(thread other msg.user),
          PrivateMessage(
            PrivateMessage.Sender(thread other msg.user),
            PrivateMessage.Text(shorten(msg.text, 80))
          )
        )
      }
    }
}
