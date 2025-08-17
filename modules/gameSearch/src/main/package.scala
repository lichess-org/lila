package lila.gameSearch

import lila.search.spec.{ DateRange, IntRange }

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("gameSearch")

val index = lila.search.Index.Game

val perfKeys: List[PerfKey] = PerfKey.list.filter: p =>
  p != PerfKey.puzzle && p != PerfKey.standard

extension (range: IntRange) def nonEmpty: Boolean = range.a.nonEmpty || range.b.nonEmpty
extension (range: DateRange) def nonEmpty: Boolean = range.a.nonEmpty || range.b.nonEmpty

extension (query: lila.search.spec.Query.Game)
  def nonEmpty: Boolean =
    query.user1.nonEmpty ||
      query.user2.nonEmpty ||
      query.winner.nonEmpty ||
      query.loser.nonEmpty ||
      query.winnerColor.nonEmpty ||
      query.perf.nonEmpty ||
      query.source.nonEmpty ||
      query.status.nonEmpty ||
      query.turns.nonEmpty ||
      query.averageRating.nonEmpty ||
      query.hasAi.nonEmpty ||
      query.aiLevel.nonEmpty ||
      query.rated.nonEmpty ||
      query.date.nonEmpty ||
      query.duration.nonEmpty ||
      query.analysed.nonEmpty ||
      query.clockInit.nonEmpty ||
      query.clockInc.nonEmpty

  def userIds: Set[UserId] =
    Set(query.user1, query.user2, query.winner, query.loser, query.whiteUser, query.blackUser).flatten
      .map(UserId.apply)
