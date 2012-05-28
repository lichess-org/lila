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
    maxPerPage: Int) {

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
    _ ← threadOption.filter(_ isUnReadBy me).fold(
      thread ⇒ for {
        _ ← threadRepo setRead thread
        _ ← io(unreadCache invalidate me)
      } yield (),
      io()
    )
  } yield threadOption

  def makeThread(data: DataForm.ThreadData, me: User) = {
    val thread = Thread(
      name = data.subject,
      text = data.text,
      creator = me,
      invited = data.user)
    for {
      _ ← threadRepo saveIO thread
      _ ← io(unreadCache invalidate data.user)
    } yield thread
  }

  def makePost(thread: Thread, text: String, me: User) = {
    val post = Post(
      text = text,
      isByCreator = thread isCreator me)
    val newThread = thread + post
    for {
      _ ← threadRepo saveIO thread
      otherUser ← userRepo byId (thread otherUserId me)
      _ ← otherUser.fold(
        other ⇒ io(unreadCache invalidate other),
        io()
      )
    } yield thread
  }
}
