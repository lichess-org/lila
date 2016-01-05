package lila.message

import akka.pattern.pipe

import lila.common.paginator._
import lila.db.api._
import lila.db.Implicits._
import lila.db.paginator._
import lila.hub.actorApi.message._
import lila.hub.actorApi.SendTo
import lila.security.Granter
import lila.user.{ User, UserRepo }
import tube.threadTube

final class Api(
    unreadCache: UnreadCache,
    shutup: akka.actor.ActorSelection,
    maxPerPage: Int,
    blocks: (String, String) => Fu[Boolean],
    bus: lila.common.Bus) {

  def inbox(me: User, page: Int): Fu[Paginator[Thread]] = Paginator(
    adapter = new Adapter(
      selector = ThreadRepo visibleByUserQuery me.id,
      sort = Seq(ThreadRepo.recentSort)
    ),
    currentPage = page,
    maxPerPage = maxPerPage
  )

  def preview(userId: String): Fu[List[Thread]] = unreadCache(userId) flatMap { ids =>
    $find byOrderedIds ids
  }

  def thread(id: String, me: User): Fu[Option[Thread]] = for {
    threadOption ← $find.byId(id) map (_ filter (_ hasUser me))
    _ ← threadOption.filter(_ isUnReadBy me).??(thread =>
      (ThreadRepo setRead thread) >>- updateUser(me)
    )
  } yield threadOption

  def markThreadAsRead(id: String, me: User): Funit = thread(id, me).void

  def makeThread(data: DataForm.ThreadData, me: User): Fu[Thread] =
    UserRepo named data.user.id flatMap {
      _.fold(fufail[Thread]("No such recipient")) { invited =>
        Thread.make(
          name = data.subject,
          text = data.text,
          creatorId = me.id,
          invitedId = data.user.id) |> { t =>
            val thread = me.troll.fold(t deleteFor invited, t)
            sendUnlessBlocked(thread) >>-
              updateUser(invited) >>- {
                val text = data.subject + " " + data.text
                shutup ! lila.hub.actorApi.shutup.RecordPrivateMessage(me.id, invited.id, text)
              } inject thread
          }
      }
    }

  def lichessThread(lt: LichessThread): Funit = sendUnlessBlocked(Thread.make(
    name = lt.subject,
    text = lt.message,
    creatorId = lt.from,
    invitedId = lt.to)) >> unreadCache.clear(lt.to)

  private def sendUnlessBlocked(thread: Thread): Funit =
    blocks(thread.invitedId, thread.creatorId) flatMap {
      !_ ?? $insert(thread)
    }

  def makePost(thread: Thread, text: String, me: User): Fu[Thread] = {
    val post = Post.make(
      text = text,
      isByCreator = thread isCreator me)
    if (thread endsWith post) fuccess(thread) // prevent duplicate post
    else {
      val newThread = thread + post
      $update[ThreadRepo.ID, Thread](newThread) >>- {
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

  def updateUser(user: lila.user.User) {
    if (!user.kid) (unreadCache refresh user) mapTo manifest[List[String]] foreach { ids =>
      bus.publish(SendTo(user.id, "nbm", ids.size), 'users)
    }
  }
}
