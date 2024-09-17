package lila.tournament

import org.joda.time.DateTime
import scala.concurrent.Promise

import lila.user.User

private[tournament] case class WaitingUsers(
    hash: Map[User.ID, DateTime],
    estimateTotalSeconds: Int,
    date: DateTime
) {

  private val waitSeconds: Int =
    if (estimateTotalSeconds < 30) 8
    else if (estimateTotalSeconds < 60) 10
    else (estimateTotalSeconds / 10 + 6) atMost 50 atLeast 15

  lazy val all  = hash.keySet
  lazy val size = hash.size

  def isOdd = size % 2 == 1

  // skips the most recent user if odd
  def evenNumber: Set[User.ID] = {
    if (isOdd) all - hash.maxBy(_._2.getMillis)._1
    else all
  }

  lazy val haveWaitedEnough: Boolean =
    size > 100 || {
      val since = date minusSeconds waitSeconds
      hash.count { case (_, d) => d.isBefore(since) } > 1
    }

  def update(us: Set[User.ID]) = {
    val newDate = DateTime.now
    copy(
      date = newDate,
      hash = {
        hash.view.filterKeys(us.contains) ++
          us.filterNot(hash.contains).map { _ -> newDate }
      }.toMap
    )
  }

  def hasUser(userId: User.ID) = hash contains userId
}

private[tournament] object WaitingUsers {

  def empty(estimateTotalSeconds: Int) = WaitingUsers(Map.empty, estimateTotalSeconds, DateTime.now)

  case class WithNext(waiting: WaitingUsers, next: Option[Promise[WaitingUsers]])

  def emptyWithNext(estimateTotalSeconds: Int) = WithNext(empty(estimateTotalSeconds), none)
}
