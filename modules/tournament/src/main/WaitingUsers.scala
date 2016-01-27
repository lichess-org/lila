package lila.tournament

import org.joda.time.DateTime

private[tournament] case class WaitingUsers(
    hash: Map[String, DateTime],
    clock: Option[chess.Clock],
    date: DateTime) {

  // 1+0  -> 10 -> 12
  // 3+0  -> 18 -> 18
  // 5+0  -> 26 -> 26
  // 10+0 -> 46 -> 35
  private val waitSeconds = {
    (clock.fold(60)(_.estimateTotalTime) / 15) + 6
  } min 35 max 12

  lazy val all = hash.keys.toList
  lazy val size = hash.size

  def isOdd = size % 2 == 1

  // skips the most recent user if odd
  def evenNumber: List[String] = {
    if (isOdd) hash.toList.sortBy(-_._2.getMillis).drop(1).map(_._1)
    else all
  }

  def waitSecondsOf(userId: String) = hash get userId map { d =>
    nowSeconds - d.getSeconds
  }

  def waiting = {
    val since = date minusSeconds waitSeconds
    hash.collect {
      case (u, d) if d.isBefore(since) => u
    }.toList
  }

  def update(us: Set[String], clock: Option[chess.Clock]) = {
    val newDate = DateTime.now
    copy(
      date = newDate,
      clock = clock,
      hash = hash.filterKeys(us.contains) ++
        us.filterNot(hash.contains).map { _ -> newDate }
    )
  }

  def intersect(us: Seq[String]) = copy(hash = hash filterKeys us.contains)

  def diff(us: Set[String]) = copy(hash = hash filterKeys { k => !us.contains(k) })

  override def toString = all.toString
}

private[tournament] object WaitingUsers {
  def empty = WaitingUsers(Map.empty, none, DateTime.now)
}
