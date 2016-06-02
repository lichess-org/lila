package lila.message

import akka.pattern.pipe

import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator._
import lila.hub.actorApi.message._
import lila.hub.actorApi.SendTo
import lila.security.Granter
import lila.user.{ User, UserRepo }

final class Api(
    coll: Coll,
    shutup: akka.actor.ActorSelection,
    maxPerPage: Int,
    blocks: (String, String) => Fu[Boolean],
    notifyApi: lila.notify.NotifyApi) {

  import Thread.ThreadBSONHandler

  def inbox(me: User, page: Int): Fu[Paginator[Thread]] = Paginator(
    adapter = new Adapter(
      collection = coll,
      selector = ThreadRepo visibleByUserQuery me.id,
      projection = $empty,
      sort = ThreadRepo.recentSort),
    currentPage = page,
    maxPerPage = maxPerPage
  )

  def thread(id: String, me: User): Fu[Option[Thread]] = for {
    threadOption ← coll.byId[Thread](id) map (_ filter (_ hasUser me))
    _ ← threadOption.filter(_ isUnReadBy me).??(ThreadRepo.setRead)
  } yield threadOption

  def markThreadAsRead(id: String, me: User): Funit = thread(id, me).void

  def makeThread(data: DataForm.ThreadData, me: User): Fu[Thread] = {
    val fromMod = Granter(_.MessageAnyone)(me)
    UserRepo named data.user.id flatMap {
      _.fold(fufail[Thread]("No such recipient")) { invited =>
        Thread.make(
          name = data.subject,
          text = data.text,
          creatorId = me.id,
          invitedId = data.user.id) |> { t =>
            val thread = if (me.troll || lila.security.Spam.detect(data.subject, data.text))
              t deleteFor invited
            else t
            sendUnlessBlocked(thread, fromMod) flatMap {
              _ ?? {
                val text = s"${data.subject} ${data.text}"
                shutup ! lila.hub.actorApi.shutup.RecordPrivateMessage(me.id, invited.id, text)
                notify(thread)
              }
            } inject thread
          }
      }
    }
  }

  def lichessThread(lt: LichessThread): Funit = {
    val thread = Thread.make(
      name = lt.subject,
      text = lt.message,
      creatorId = lt.from,
      invitedId = lt.to)
    sendUnlessBlocked(thread, fromMod = false) flatMap { sent =>
      sent ?? notify(thread)
    }
  }

  private def sendUnlessBlocked(thread: Thread, fromMod: Boolean): Fu[Boolean] =
    if (fromMod) coll.insert(thread) inject true
    else blocks(thread.invitedId, thread.creatorId) flatMap { blocks =>
      ((!blocks) ?? coll.insert(thread).void) inject !blocks
    }

  def makePost(thread: Thread, text: String, me: User): Fu[Thread] = {
    val post = Post.make(
      text = text,
      isByCreator = thread isCreator me)
    if (thread endsWith post) fuccess(thread) // prevent duplicate post
    else blocks(thread receiverOf post, me.id) flatMap {
      case true => fuccess(thread)
      case false =>
        val newThread = thread + post
        coll.update($id(newThread.id), newThread) >> {
          val toUserId = newThread otherUserId me
          shutup ! lila.hub.actorApi.shutup.RecordPrivateMessage(me.id, toUserId, text)
          notify(thread, post)
        } inject newThread
    }
  }

  def deleteThread(id: String, me: User): Funit =
    thread(id, me) flatMap { threadOption =>
      threadOption.map(_.id) ?? (ThreadRepo deleteFor me.id)
    }

  def notify(thread: Thread): Funit = thread.posts.headOption ?? { post =>
    notify(thread, post)
  }
  def notify(thread: Thread, post: Post): Funit = {
    import lila.notify.{ Notification, PrivateMessage }
    notifyApi addNotification Notification(
      Notification.Notifies(thread receiverOf post),
      PrivateMessage(
        PrivateMessage.SenderId(thread senderOf post),
        PrivateMessage.Thread(id = thread.id, name = thread.name),
        PrivateMessage.Text(post.text take 100)))
  }
}
