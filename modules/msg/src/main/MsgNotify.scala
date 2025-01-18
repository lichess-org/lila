package lila.msg

import akka.actor.Cancellable

import lila.common.String.shorten
import lila.core.notify.{ NotifyApi, NotificationContent }
import lila.db.dsl.{ *, given }

final private class MsgNotify(
    colls: MsgColls,
    notifyApi: NotifyApi
)(using ec: Executor, scheduler: Scheduler):

  import BsonHandlers.given

  private val delay = 5.seconds

  private val delayed = scalalib.ConcurrentMap[MsgThread.Id, Cancellable](256)

  def onPost(threadId: MsgThread.Id): Unit = schedule(threadId)

  def onRead(threadId: MsgThread.Id, userId: UserId, contactId: UserId): Funit =
    (!cancel(threadId)).so:
      notifyApi
        .markRead(
          userId,
          $doc(
            "content.type" -> "privateMessage",
            "content.user" -> contactId
          )
        )
        .void

  def deleteAllBy(threads: List[MsgThread], user: User): Funit =
    threads.sequentiallyVoid { thread =>
      cancel(thread.id)
      notifyApi.remove(thread.other(user.id), $doc("content.user" -> user.id)).void
    }

  private def schedule(threadId: MsgThread.Id): Unit = delayed.compute(threadId): canc =>
    canc.foreach(_.cancel())
    scheduler
      .scheduleOnce(delay):
        delayed.remove(threadId)
        doNotify(threadId)
      .some

  private def cancel(threadId: MsgThread.Id): Boolean =
    delayed.remove(threadId).map(_.cancel()).isDefined

  private def doNotify(threadId: MsgThread.Id): Funit =
    colls.thread.byId[MsgThread](threadId.value).flatMapz { thread =>
      val msg = thread.lastMsg
      (!thread.delBy(thread.other(msg.user))).so:
        notifyApi.notifyOne(
          thread.other(msg.user),
          NotificationContent.PrivateMessage(msg.user, text = shorten(msg.text, 40))
        )
    }
