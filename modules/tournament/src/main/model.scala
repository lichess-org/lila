package lila.tournament

import org.joda.time.DateTime

case class MiniStanding(
  tour: Tournament,
  standing: Option[RankedPlayers])

case class PlayerInfo(
    rank: Int,
    withdraw: Boolean) {
  def page = {
    math.floor((rank - 1) / 10) + 1
  }.toInt
}

private[tournament] case class WaitingUsers(
    hash: Map[String, DateTime],
    clock: Option[chess.Clock],
    date: DateTime) {

  // 1+0  -> 5  -> 7
  // 3+0  -> 9  -> 11
  // 5+0  -> 17 -> 17
  // 10+0 -> 32 -> 30
  private val waitSeconds = {
    (clock.fold(60)(_.estimateTotalTime) / 20) + 2
  } min 30 max 7

  lazy val all = hash.keys.toList

  def waiting = {
    val since = date minusSeconds waitSeconds
    hash.collect {
      case (u, d) if d.isBefore(since) => u
    }.toList
  }

  def newestId: Option[String] =
    hash.toList.sortBy(-_._2.getMillis).headOption.map(_._1)

  def update(us: Seq[String], clock: Option[chess.Clock]) = {
    val newDate = DateTime.now
    copy(
      date = newDate,
      clock = clock,
      hash = hash.filterKeys(us.contains) ++
        us.filterNot(hash.contains).map { _ -> newDate }
    )
  }

  def intersect(us: List[String]) = copy(hash = hash filterKeys us.contains)
}
private[tournament] object WaitingUsers {
  def empty = WaitingUsers(Map.empty, none, DateTime.now)
}
