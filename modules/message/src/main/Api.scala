package lila.message

import lila.user.{ User, UserRepo }
import lila.common.paginator._
import lila.db.paginator._
import lila.db.Implicits._
import lila.db.api._
import lila.hub.actorApi.SendTo
import lila.hub.actorApi.message._
import tube.threadTube

import akka.actor.ActorRef
import scala.math.ceil
import play.api.libs.concurrent.Execution.Implicits._

final class Api(
    unreadCache: UnreadCache,
    maxPerPage: Int,
    socketHub: ActorRef) {

  def inbox(me: User, page: Int): Fu[Paginator[Thread]] = Paginator(
    adapter = new Adapter(
      selector = ThreadRepo visibleByUserQuery me.id,
      sort = Seq(ThreadRepo.recentSort)
    ),
    currentPage = page,
    maxPerPage = maxPerPage
  )

  def thread(id: String, me: User): Fu[Option[Thread]] = for {
    threadOption ← $find.byId(id) map (_ filter (_ hasUser me))
    _ ← threadOption.filter(_ isUnReadBy me).zmap(thread ⇒
      (ThreadRepo setRead thread) >> updateUser(me.id)
    )
  } yield threadOption

  def makeThread(data: DataForm.ThreadData, me: User): Fu[Thread] = {
    val thread = Thread.make(
      name = data.subject,
      text = data.text,
      creatorId = me.id,
      invitedId = data.user.id)
    $insert(thread) >> updateUser(data.user.id) inject thread
  }

  def lichessThread(lt: LichessThread): Funit = Thread.make(
    name = lt.subject,
    text = lt.message,
    creatorId = "lichess",
    invitedId = lt.to) |> { thread ⇒ $insert(thread) >> updateUser(lt.to) }

  def makePost(thread: Thread, text: String, me: User) = {
    val post = Post.make(
      text = text,
      isByCreator = thread isCreator me)
    val newThread = thread + post
    for {
      _ ← $update[ThreadRepo.ID, Thread](newThread)
      receiver ← UserRepo.named(thread receiverOf post)
      _ ← receiver.map(_.id) zmap updateUser
    } yield newThread
  }

  def deleteThread(id: String, me: User): Funit =
    thread(id, me) flatMap { threadOption ⇒
      threadOption.map(_.id).zmap(ThreadRepo deleteFor me.id)
    }

  val nbUnreadMessages = unreadCache.apply _

  private def updateUser(user: String): Funit = {
    (unreadCache refresh user) onSuccess {
      case nb ⇒ socketHub ! SendTo(user, "nbm", nb)
    }
    funit
  }
}
