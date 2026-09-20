package lila.gameSearch

import org.apache.pekko.stream.scaladsl.*

import lila.search.{ SearchClient, SearchApi }

// see file://./../../../../bin/elastic/game.ts

final class GameSearchApi(
    client: SearchClient,
    gameRepo: lila.core.game.GameRepo,
    userApi: lila.core.user.UserApi
)(using Executor)
    extends SearchApi[Game, Query]:
  import SearchClient.*

  def search(query: Query, offset: Long, length: Long): Fu[List[Game]] =
    client
      .searchIds(
        Index.Game,
        makeQuery(query),
        makeSort(query),
        offset,
        length,
        query,
        timeout = 5.seconds.some
      )
      .flatMap: ids =>
        gameRepo.gamesFromSecondary(ids.map(GameId.apply))

  def count(query: Query): Fu[Long] =
    client.count(Index.Game, makeQuery(query), query)

  def validateAccounts(query: Query, forMod: Boolean): Fu[Boolean] =
    fuccess(forMod) >>| userApi.containsDisabled(query.userIds).not

  def idStream(query: Query, total: Long, batchSize: MaxPerPage): Source[List[GameId], ?] =
    Source.unfoldAsync(0L): offset =>
      if offset >= total then fuccess(none)
      else
        val length = math.min(batchSize.value.toLong, total - offset)
        client
          .searchIds(
            Index.Game,
            makeQuery(query),
            makeSort(query),
            offset,
            length,
            query,
            timeout = 5.seconds.some
          )
          .map: ids =>
            Option.when(ids.nonEmpty):
              (offset + length) -> ids.map(GameId.apply)

  private def makeQuery(query: Query): SearchQuery =
    compileFilter(
      userQueries(query) ++
        winnerQueries(query) ++
        loserQueries(query) ++
        query.winnerColor.map(term(Fields.winnerColor, _)).toList ++
        intRange(Fields.turns, query.turns) ++
        averageRatingQueries(query) ++
        intRange(Fields.duration, query.duration) ++
        query.clockInit.map(term(Fields.clockInit, _)).toList ++
        query.clockInc.map(term(Fields.clockInc, _)).toList ++
        dateRange(Fields.date, query.date) ++
        hasAiQueries(query) ++
        aiLevelQueries(query) ++
        query.perf.nonEmpty.option(terms(Fields.perf, query.perf)).toList ++
        query.source.map(term(Fields.source, _)).toList ++
        query.rated.map(term(Fields.rated, _)).toList ++
        query.status.map(statusQuery).toList ++
        query.analysed.map(term(Fields.analysed, _)).toList ++
        query.whiteUser.map(termLower(Fields.whiteUser, _)).toList ++
        query.blackUser.map(termLower(Fields.blackUser, _)).toList
    )

  private def makeSort(query: Query): List[SearchSort] =
    List(
      fieldSort(
        Sorting.fieldOrDefault(query.sorting.field),
        Sorting.orderOrDefault(query.sorting.order)
      )
    )

  private def userQueries(query: Query): List[SearchQuery] =
    (query.user1, query.user2) match
      case (Some(user1), Some(user2)) =>
        List(
          boolQuery(
            should = List(
              boolQuery(must = List(termLower(Fields.whiteUser, user1), termLower(Fields.blackUser, user2))),
              boolQuery(must = List(termLower(Fields.whiteUser, user2), termLower(Fields.blackUser, user1)))
            ),
            minimumShouldMatch = 1.some
          )
        )
      case _ =>
        List(query.user1, query.user2).flatten.map: user =>
          boolQuery(
            should = List(termLower(Fields.whiteUser, user), termLower(Fields.blackUser, user)),
            minimumShouldMatch = 1.some
          )

  private def winnerQueries(query: Query): List[SearchQuery] =
    query.winner.toList.map: winner =>
      boolQuery(
        should = List(
          boolQuery(must = List(termLower(Fields.whiteUser, winner), term(Fields.winnerColor, 1))),
          boolQuery(must = List(termLower(Fields.blackUser, winner), term(Fields.winnerColor, 2)))
        ),
        minimumShouldMatch = 1.some
      )

  private def loserQueries(query: Query): List[SearchQuery] =
    query.loser.toList.map: loser =>
      boolQuery(
        should = List(
          boolQuery(must = List(termLower(Fields.whiteUser, loser), term(Fields.winnerColor, 2))),
          boolQuery(must = List(termLower(Fields.blackUser, loser), term(Fields.winnerColor, 1)))
        ),
        minimumShouldMatch = 1.some
      )

  private def averageRatingQueries(query: Query): List[SearchQuery] =
    if query.averageRating.nonEmpty then
      numberRange(Fields.averageRating, gt = 0d.some) :: intRange(Fields.averageRating, query.averageRating)
    else Nil

  private def hasAiQueries(query: Query): List[SearchQuery] =
    query.hasAi.toList.map:
      case true => numberRange(Fields.ai, gt = 0d.some)
      case false => term(Fields.ai, 0)

  private def aiLevelQueries(query: Query): List[SearchQuery] =
    if query.hasAi.contains(false) then Nil else intRange(Fields.ai, query.aiLevel)

  private def statusQuery(status: Int): SearchQuery =
    if status == chess.Status.Draw.id then
      terms(
        Fields.status,
        List(
          chess.Status.Stalemate.id,
          chess.Status.Draw.id,
          chess.Status.InsufficientMaterialClaim.id
        )
      )
    else term(Fields.status, status)

  private def termLower(field: String, value: String): SearchQuery =
    term(field, value.toLowerCase)

  private def intRange(field: String, range: IntRange): List[SearchQuery] =
    val sorted = range.sorted
    Option
      .when(sorted.a.nonEmpty || sorted.b.nonEmpty):
        numberRange(field, gte = sorted.a.map(_.toDouble), lte = sorted.b.map(_.toDouble))
      .toList

  private def dateRange(field: String, range: DateRange): List[SearchQuery] =
    val sorted = range.sorted
    Option
      .when(sorted.a.nonEmpty || sorted.b.nonEmpty):
        numberRange(
          field,
          gte = sorted.a.map(_.getEpochSecond.toDouble),
          lte = sorted.b.map(_.getEpochSecond.toDouble)
        )
      .toList
