package lila
package message

import user.{ User, UserRepo }

import scalaz.effects._
import com.github.ornicar.paginator._
import scala.math.ceil

final class Api(
    threadRepo: ThreadRepo,
    unreadCache: UnreadCache,
    userRepo: UserRepo,
    maxPerPage: Int,
    notifyUnread: (String, Int) ⇒ Unit) {

  def inbox(me: User, page: Int): Paginator[Thread] = Paginator(
    SalatAdapter(
      dao = threadRepo,
      query = threadRepo visibleByUserQuery me,
      sort = threadRepo.sortQuery),
    currentPage = page,
    maxPerPage = maxPerPage
  ) | inbox(me, 1)

  def thread(id: String, me: User): IO[Option[Thread]] = for {
    threadOption ← threadRepo byId id map (_ filter (_ hasUser me))
    _ ← ~threadOption.filter(_ isUnReadBy me).map(thread ⇒ 
      (threadRepo setRead thread) >> updateUser(me)
    )
  } yield threadOption

  def makeThread(data: DataForm.ThreadData, me: User) = {
    val thread = Thread.make(
      name = data.subject,
      text = data.text,
      creator = me.id,
      invited = data.user.id)
    (threadRepo saveIO thread) >> updateUser(data.user.id) inject thread
  }

  def lichessThread(lt: LichessThread): IO[Unit] =
    (threadRepo saveIO lt.toThread) >> updateUser(lt.to)

  def makePost(thread: Thread, text: String, me: User) = {
    val post = Post.make(
      text = text,
      isByCreator = thread isCreator me)
    val newThread = thread + post
    for {
      _ ← threadRepo saveIO newThread
      receiver ← userRepo byId (thread receiverOf post)
      _ ← receiver.fold(io())(updateUser)
    } yield thread
  }

  private def updateUser(user: String): IO[Unit] = io {
    notifyUnread(user, unreadCache refresh user)
  }
  private def updateUser(user: User): IO[Unit] = updateUser(user.id)

  def deleteThread(id: String, me: User): IO[Unit] = for {
    threadOption ← thread(id, me)
    _ ← ~threadOption.map(threadRepo.deleteFor(me))
  } yield ()
}
