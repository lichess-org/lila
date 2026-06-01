package lila.gameSearch

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.search.{ SearchClient, SearchApi }

// see file://./../../../../bin/elastic/game.ts

final class GameSearchApi(
    elastic: SearchClient,
    gameRepo: lila.core.game.GameRepo,
    userApi: lila.core.user.UserApi
)(using Executor)
    extends SearchApi[Game, Query]:

  import SearchClient.*

  def search(query: Query, offset: Long, length: Long): Fu[List[Game]] =
    elastic
      .searchIds(Index.Game, makeQuery(query), makeSort(query), offset, length, query)
      .flatMap: ids =>
        gameRepo.gamesFromSecondary(ids.map(GameId.apply))

  def count(query: Query): Fu[Long] =
    elastic.count(Index.Game, makeQuery(query), query)

  def validateAccounts(query: Query, forMod: Boolean): Fu[Boolean] =
    fuccess(forMod) >>| userApi.containsDisabled(query.userIds).not

  def idStream(query: Query, total: Long, batchSize: MaxPerPage): Source[List[GameId], ?] =
    Source.unfoldAsync(0L): offset =>
      if offset >= total then fuccess(none)
      else
        val length = math.min(batchSize.value.toLong, total - offset)
        elastic
          .searchIds(Index.Game, makeQuery(query), makeSort(query), offset, length, query)
          .map: ids =>
            Option.when(ids.nonEmpty):
              (offset + length) -> ids.map(GameId.apply)

  private def makeQuery(query: Query): JsObject =
    compileFilter(
      userQueries(query) ++
        winnerQueries(query) ++
        loserQueries(query) ++
        query.winnerColor.map(termInt(Fields.winnerColor, _)).toList ++
        intRange(Fields.turns, query.turns) ++
        averageRatingQueries(query) ++
        intRange(Fields.duration, query.duration) ++
        query.clockInit.map(termInt(Fields.clockInit, _)).toList ++
        query.clockInc.map(termInt(Fields.clockInc, _)).toList ++
        dateRange(Fields.date, query.date) ++
        hasAiQueries(query) ++
        aiLevelQueries(query) ++
        query.perf.nonEmpty.option(termsInt(Fields.perf, query.perf)).toList ++
        query.source.map(termInt(Fields.source, _)).toList ++
        query.rated.map(term(Fields.rated, _)).toList ++
        query.status.map(termInt(Fields.status, _)).toList ++
        query.analysed.map(term(Fields.analysed, _)).toList ++
        query.whiteUser.map(termLower(Fields.whiteUser, _)).toList ++
        query.blackUser.map(termLower(Fields.blackUser, _)).toList
    )

  private def makeSort(query: Query): JsArray =
    Json.arr(
      fieldSort(
        Sorting.fieldOrDefault(query.sorting.field),
        Sorting.orderOrDefault(query.sorting.order)
      )
    )

  private def userQueries(query: Query): List[JsObject] =
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

  private def winnerQueries(query: Query): List[JsObject] =
    query.winner.toList.map: winner =>
      boolQuery(
        should = List(
          boolQuery(must = List(termLower(Fields.whiteUser, winner), termInt(Fields.winnerColor, 1))),
          boolQuery(must = List(termLower(Fields.blackUser, winner), termInt(Fields.winnerColor, 2)))
        ),
        minimumShouldMatch = 1.some
      )

  private def loserQueries(query: Query): List[JsObject] =
    query.loser.toList.map: loser =>
      boolQuery(
        should = List(
          boolQuery(must = List(termLower(Fields.whiteUser, loser), termInt(Fields.winnerColor, 2))),
          boolQuery(must = List(termLower(Fields.blackUser, loser), termInt(Fields.winnerColor, 1)))
        ),
        minimumShouldMatch = 1.some
      )

  private def averageRatingQueries(query: Query): List[JsObject] =
    if query.averageRating.nonEmpty then
      rangeGt(Fields.averageRating, 0) :: intRange(Fields.averageRating, query.averageRating)
    else Nil

  private def hasAiQueries(query: Query): List[JsObject] =
    query.hasAi.toList.map:
      case true => rangeGt(Fields.ai, 0)
      case false => termInt(Fields.ai, 0)

  private def aiLevelQueries(query: Query): List[JsObject] =
    if query.hasAi.contains(false) then Nil else intRange(Fields.ai, query.aiLevel)

  private def termLower(field: String, value: String): JsObject =
    term(field, value.toLowerCase)

  private def termInt(field: String, value: Int): JsObject =
    Json.obj("term" -> Json.obj(field -> value))

  private def termsInt(field: String, values: List[Int]): JsObject =
    Json.obj("terms" -> Json.obj(field -> values))

  private def intRange(field: String, range: IntRange): List[JsObject] =
    val sorted = range.sorted
    val bounds = Json.obj() ++
      sorted.a.map(value => Json.obj("gte" -> value)).getOrElse(Json.obj()) ++
      sorted.b.map(value => Json.obj("lte" -> value)).getOrElse(Json.obj())
    if bounds.keys.isEmpty then Nil else List(Json.obj("range" -> Json.obj(field -> bounds)))

  private def dateRange(field: String, range: DateRange): List[JsObject] =
    val sorted = range.sorted
    val bounds = Json.obj() ++
      sorted.a.map(value => Json.obj("gte" -> value.getEpochSecond)).getOrElse(Json.obj()) ++
      sorted.b.map(value => Json.obj("lte" -> value.getEpochSecond)).getOrElse(Json.obj())
    if bounds.keys.isEmpty then Nil else List(Json.obj("range" -> Json.obj(field -> bounds)))

  private def rangeGt(field: String, value: Int): JsObject =
    Json.obj("range" -> Json.obj(field -> Json.obj("gt" -> value)))
