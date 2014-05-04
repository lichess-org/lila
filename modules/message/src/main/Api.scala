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
    maxPerPage: Int,
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
      (ThreadRepo setRead thread) >>- updateUser(me.id)
    )
  } yield threadOption

  def makeThread(data: DataForm.ThreadData, me: User): Fu[Thread] =
    UserRepo named data.user.id flatMap {
      _.fold(fufail[Thread]("No such recipient")) { invited =>
        Thread.make(
          name = data.subject,
          text = data.text,
          creatorId = me.id,
          invitedId = data.user.id) |> { t =>
            val thread = (me.troll && !Granter(_.MarkTroll)(invited)).fold(
              t deleteFor invited,
              t)
            $insert(thread) >>- updateUser(invited.id) inject thread
          }
      }
    }

  def lichessThread(lt: LichessThread): Funit = Thread.make(
    name = lt.subject,
    text = lt.message,
    creatorId = "lichess",
    invitedId = lt.to) |> { thread => $insert(thread) >>- updateUser(lt.to) }

  def makePost(thread: Thread, text: String, me: User) = {
    val post = Post.make(
      text = text,
      isByCreator = thread isCreator me)
    val newThread = thread + post
    $update[ThreadRepo.ID, Thread](newThread) >>- {
      UserRepo.named(thread receiverOf post) foreach {
        _.map(_.id) foreach updateUser
      }
    } inject newThread
  }

  def deleteThread(id: String, me: User): Funit =
    thread(id, me) flatMap { threadOption =>
      (threadOption.map(_.id) ?? (ThreadRepo deleteFor me.id)) >>-
        updateUser(me.id)
    }

  val unreadIds = unreadCache apply _

  def updateUser(user: String) {
    (unreadCache refresh user) mapTo manifest[List[String]] foreach { ids =>
      bus.publish(SendTo(user, "nbm", ids.size), 'users)
    }
  }
}
