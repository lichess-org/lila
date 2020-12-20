package lila.tournament

import org.joda.time.DateTime
import scala.concurrent.Promise

import chess.Clock.{ Config => TournamentClock }
import lila.user.User

private[tournament] case class WaitingUsers(
    hash: Map[User.ID, DateTime],
    clock: TournamentClock,
    date: DateTime
) {

  // ultrabullet -> 8
  // hyperbullet -> 10
  // 1+0  -> 12  -> 15
  // 3+0  -> 24  -> 24
  // 5+0  -> 36  -> 36
  // 10+0 -> 66  -> 50
  private val waitSeconds: Int =
    if (clock.estimateTotalSeconds < 30) 8
    else if (clock.estimateTotalSeconds < 60) 10
    else {
      clock.estimateTotalSeconds / 10 + 6
    } atMost 50 atLeast 15

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

  def empty(clock: TournamentClock) = WaitingUsers(Map.empty, clock, DateTime.now)

  case class WithNext(waiting: WaitingUsers, next: Option[Promise[WaitingUsers]])

  def emptyWithNext(clock: TournamentClock) = WithNext(empty(clock), none)
}
