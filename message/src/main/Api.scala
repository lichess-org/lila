package lila.message

import lila.user.{ User, UserRepo }
import lila.common.paginator._
import lila.db.paginator._
import lila.db.Implicits.docId

import scala.math.ceil
import play.api.libs.concurrent.Execution.Implicits._

final class Api(
    threadRepo: ThreadRepo,
    unreadCache: UnreadCache,
    userRepo: UserRepo,
    maxPerPage: Int,
    notifyUnread: (String, Int) ⇒ Unit) {

  def inbox(me: User, page: Int): Fu[Paginator[Thread]] = Paginator(
    new Adapter(
      repo = threadRepo,
      query = threadRepo visibleByUserQuery me.id,
      sort = Seq(threadRepo.recentSort)),
    currentPage = page,
    maxPerPage = maxPerPage
  )

  def thread(id: String, me: User): Fu[Option[Thread]] = for {
    threadOption ← threadRepo.find byId id map (_ filter (_ hasUser me))
    _ ← threadOption.filter(_ isUnReadBy me).zmap(thread ⇒
      (threadRepo setRead thread) >> updateUser(me.id)
    )
  } yield threadOption

  def makeThread(data: DataForm.ThreadData, me: User): Fu[Thread] = {
    val thread = Threads.make(
      name = data.subject,
      text = data.text,
      creatorId = me.id,
      invitedId = data.user.id)
    (threadRepo insert thread) >> updateUser(data.user.id) inject thread
  }

  def lichessThread(lt: LichessThread): Funit =
    (threadRepo insert lt.toThread) >> updateUser(lt.to)

  def makePost(thread: Thread, text: String, me: User) = {
    val post = Posts.make(
      text = text,
      isByCreator = thread isCreator me)
    val newThread = thread + post
    for {
      _ ← threadRepo update newThread
      receiver ← userRepo.find byId (thread receiverOf post)
      _ ← receiver.map(_.id) zmap updateUser
    } yield thread
  }

  private def updateUser(user: String): Funit = {
    (unreadCache refresh user) onSuccess {
      case nb ⇒ notifyUnread(user, nb)
    }
    funit
  }

  def deleteThread(id: String, me: User): Funit =
    thread(id, me) flatMap { threadOption ⇒
      threadOption.map(_.id).zmap(threadRepo deleteFor me.id)
    }
}
