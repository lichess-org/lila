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
    unreadCache: UnreadCache,
    shutup: akka.actor.ActorSelection,
    maxPerPage: Int,
    blocks: (String, String) => Fu[Boolean],
    bus: lila.common.Bus) {

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

  def preview(userId: String): Fu[List[Thread]] = unreadCache(userId) flatMap { ids =>
    coll.byOrderedIds[Thread](ids)(_.id)
  }

  def thread(id: String, me: User): Fu[Option[Thread]] = for {
    threadOption ← coll.byId[Thread](id) map (_ filter (_ hasUser me))
    _ ← threadOption.filter(_ isUnReadBy me).??(thread =>
      (ThreadRepo setRead thread) >>- updateUser(me)
    )
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
            sendUnlessBlocked(thread, fromMod) >>-
              updateUser(invited) >>- {
                val text = s"${data.subject} ${data.text}"
                shutup ! lila.hub.actorApi.shutup.RecordPrivateMessage(me.id, invited.id, text)
              } inject thread
          }
      }
    }
  }

  def lichessThread(lt: LichessThread): Funit = sendUnlessBlocked(Thread.make(
    name = lt.subject,
    text = lt.message,
    creatorId = lt.from,
    invitedId = lt.to), fromMod = false) >> {
      if (lt.notification) updateUser(lt.to)
      else unreadCache.clear(lt.to)
  }

  private def sendUnlessBlocked(thread: Thread, fromMod: Boolean): Funit =
    if (fromMod) coll.insert(thread).void
    else blocks(thread.invitedId, thread.creatorId) flatMap {
      !_ ?? coll.insert(thread).void
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
        coll.update($id(newThread.id), newThread) >>- {
          UserRepo.named(thread receiverOf post) foreach {
            _ foreach updateUser
          }
        } >>- {
          val toUserId = newThread otherUserId me
          shutup ! lila.hub.actorApi.shutup.RecordPrivateMessage(me.id, toUserId, text)
        } inject newThread
    }
  }

  def deleteThread(id: String, me: User): Funit =
    thread(id, me) flatMap { threadOption =>
      (threadOption.map(_.id) ?? (ThreadRepo deleteFor me.id)) >>-
        updateUser(me)
    }

  val unreadIds = unreadCache apply _

  def updateUser(user: User) {
    if (!user.kid) (unreadCache refresh user.id) mapTo manifest[List[String]] foreach { ids =>
      bus.publish(SendTo(user.id, "nbm", ids.size), 'users)
    }
  }
}
