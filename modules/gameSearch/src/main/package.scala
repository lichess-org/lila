package lila.gameSearch

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

val perfKeys: List[PerfKey] = PerfKey.list.filter: p =>
  p != PerfKey.puzzle && p != PerfKey.standard

case class IntRange(a: Option[Int] = None, b: Option[Int] = None):
  def nonEmpty: Boolean = a.nonEmpty || b.nonEmpty
  def sorted: IntRange =
    (a, b) match
      case (Some(min), Some(max)) if min > max => IntRange(max.some, min.some)
      case _ => this

case class DateRange(a: Option[Instant] = None, b: Option[Instant] = None):
  def nonEmpty: Boolean = a.nonEmpty || b.nonEmpty
  def sorted: DateRange =
    (a, b) match
      case (Some(min), Some(max)) if min.isAfter(max) => DateRange(max.some, min.some)
      case _ => this

case class Query(
    user1: Option[String] = None,
    user2: Option[String] = None,
    winner: Option[String] = None,
    loser: Option[String] = None,
    winnerColor: Option[Int] = None,
    perf: List[Int] = Nil,
    source: Option[Int] = None,
    status: Option[Int] = None,
    turns: IntRange = IntRange(),
    averageRating: IntRange = IntRange(),
    hasAi: Option[Boolean] = None,
    aiLevel: IntRange = IntRange(),
    rated: Option[Boolean] = None,
    date: DateRange = DateRange(),
    duration: IntRange = IntRange(),
    sorting: SearchSort = SearchSort(),
    analysed: Option[Boolean] = None,
    whiteUser: Option[String] = None,
    blackUser: Option[String] = None,
    clockInit: Option[Int] = None,
    clockInc: Option[Int] = None
):
  def nonEmpty: Boolean =
    user1.nonEmpty ||
      user2.nonEmpty ||
      winner.nonEmpty ||
      loser.nonEmpty ||
      winnerColor.nonEmpty ||
      perf.nonEmpty ||
      source.nonEmpty ||
      status.nonEmpty ||
      turns.nonEmpty ||
      averageRating.nonEmpty ||
      hasAi.nonEmpty ||
      aiLevel.nonEmpty ||
      rated.nonEmpty ||
      date.nonEmpty ||
      duration.nonEmpty ||
      analysed.nonEmpty ||
      whiteUser.nonEmpty ||
      blackUser.nonEmpty ||
      clockInit.nonEmpty ||
      clockInc.nonEmpty

  def userIds: Set[UserId] =
    Set(user1, user2, winner, loser, whiteUser, blackUser).flatten.map(UserId.apply)
