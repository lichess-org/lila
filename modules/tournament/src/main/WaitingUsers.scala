package lila.tournament

import org.joda.time.DateTime

import chess.Clock.{ Config => TournamentClock }
import lila.user.User

private[tournament] case class WaitingUsers(
    hash: Map[User.ID, DateTime],
    clock: Option[TournamentClock],
    date: DateTime
) {

  // ultrabullet -> 9
  // hyperbullet -> 11
  // 1+0  -> 12  -> 15
  // 3+0  -> 24  -> 24
  // 5+0  -> 36  -> 36
  // 10+0 -> 66  -> 50
  private val waitSeconds: Int = clock.fold(30) { c =>
    if (c.estimateTotalSeconds < 30) 9
    else if (c.estimateTotalSeconds < 60) 11
    else {
      c.estimateTotalSeconds / 10 + 6
    } atMost 50 atLeast 15
  }

  lazy val all = hash.keys.toList
  lazy val size = hash.size

  def isOdd = size % 2 == 1

  // skips the most recent user if odd
  def evenNumber: List[User.ID] = {
    if (isOdd) hash.toList.sortBy(-_._2.getMillis).drop(1).map(_._1)
    else all
  }

  def waitSecondsOf(userId: User.ID) = hash get userId map { d =>
    nowSeconds - d.getSeconds
  }

  def waiting: List[User.ID] = {
    val since = date minusSeconds waitSeconds
    hash.collect {
      case (u, d) if d.isBefore(since) => u
    }(scala.collection.breakOut)
  }

  def update(us: Set[User.ID], clock: Option[TournamentClock]) = {
    val newDate = DateTime.now
    copy(
      date = newDate,
      clock = clock,
      hash = hash.filterKeys(us.contains) ++
        us.filterNot(hash.contains).map { _ -> newDate }
    )
  }

  def intersect(us: Set[User.ID]) = copy(hash = hash filterKeys us.contains)

  def diff(us: Set[User.ID]) = copy(hash = hash filterKeys { k => !us.contains(k) })

  override def toString = all.toString
}

private[tournament] object WaitingUsers {

  val empty = WaitingUsers(Map.empty, none, DateTime.now)
}
