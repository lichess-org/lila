package lila.message

import akka.pattern.pipe

import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator._
import lila.security.Granter
import lila.user.{ User, UserRepo }


final class MessageApi(
    coll: Coll,
    shutup: akka.actor.ActorSelection,
    maxPerPage: Int,
    blocks: (String, String) => Fu[Boolean],
    notifyApi: lila.notify.NotifyApi,
    follows: (String, String) => Fu[Boolean],
    lilaBus: lila.common.Bus) {

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
    _ ← threadOption.filter(_ isUnReadBy me).??(ThreadRepo.setReadFor(me))
  } yield threadOption

  def makeThread(data: DataForm.ThreadData, me: User): Fu[Thread] = {
    val fromMod = Granter(_.MessageAnyone)(me)
    UserRepo named data.user.id flatMap {
      _.fold(fufail[Thread]("No such recipient")) { invited =>
        val t = Thread.make(
          name = data.subject,
          text = data.text,
          creatorId = me.id,
          invitedId = data.user.id)
        muteThreadIfNecessary(t, me, invited, data) flatMap { thread =>
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

  private def muteThreadIfNecessary(thread: Thread, creator: User, invited: User, data: DataForm.ThreadData): Fu[Thread] =
    if (lila.security.Spam.detect(data.subject, data.text)) fuccess(thread deleteFor invited)
    else if (creator.troll) follows(invited.id, creator.id) map { following =>
      if (following) thread else thread deleteFor invited
    }
    else fuccess(thread)

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
    thread(id, me) flatMap {
      _ ?? { thread =>
        ThreadRepo.deleteFor(me.id)(thread.id) zip
          notifyApi.remove(
            lila.notify.Notification.Notifies(me.id),
            $doc("content.thread.id" -> thread.id)) void
      }
    }

  def notify(thread: Thread): Funit = thread.posts.headOption ?? { post =>
    notify(thread, post)
  }
  def notify(thread: Thread, post: Post): Funit =
    (thread isVisibleBy thread.receiverOf(post)) ?? {
      import lila.notify.{ Notification, PrivateMessage }
      import lila.common.String.shorten
      lilaBus.publish(Event.NewMessage(thread, post), 'newMessage)
      notifyApi addNotification Notification(
        Notification.Notifies(thread receiverOf post),
        PrivateMessage(
          PrivateMessage.SenderId(thread senderOf post),
          PrivateMessage.Thread(id = thread.id, name = shorten(thread.name, 80)),
          PrivateMessage.Text(shorten(post.text, 80))))
    }
}
