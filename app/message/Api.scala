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
    _ ← ~threadOption.filter(_ isUnReadBy me).map(thread ⇒ for {
      _ ← threadRepo setRead thread
      _ ← updateUser(me)
    } yield ())
  } yield threadOption

  def makeThread(data: DataForm.ThreadData, me: User) = {
    val thread = Thread(
      name = data.subject,
      text = data.text,
      creator = me,
      invited = data.user)
    for {
      _ ← threadRepo saveIO thread
      _ ← updateUser(data.user)
    } yield thread
  }

  def makePost(thread: Thread, text: String, me: User) = {
    val post = Post(
      text = text,
      isByCreator = thread isCreator me)
    val newThread = thread + post
    for {
      _ ← threadRepo saveIO newThread
      receiver ← userRepo byId (thread receiverOf post)
      _ ← receiver.fold(io())(updateUser)
    } yield thread
  }

  private def updateUser(user: User) = io {
    notifyUnread(user.id, unreadCache refresh user)
  }

  def deleteThread(id: String, me: User): IO[Unit] = for {
    threadOption ← thread(id, me)
    _ ← ~threadOption.map(threadRepo.deleteFor(me))
  } yield ()
}
