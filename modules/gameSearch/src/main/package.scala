package lila.gameSearch

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("gameSearch")

val index = lila.search.spec.Index.Game

extension (query: lila.search.spec.Query.Game)
  def nonEmpty =
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
