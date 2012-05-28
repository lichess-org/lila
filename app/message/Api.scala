package lila
package message

import user.User

import scalaz.effects._
import com.github.ornicar.paginator._
import scala.math.ceil

final class Api(
    threadRepo: ThreadRepo,
    unreadCache: UnreadCache,
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
}
