package lila.gameSearch

import akka.stream.scaladsl.*

import lila.search.client.SearchClient
import lila.search.spec.Query
import lila.search.{ From, SearchReadApi, Size }

final class GameSearchApi(
    client: SearchClient,
    gameRepo: lila.core.game.GameRepo,
    userApi: lila.core.user.UserApi
)(using Executor)
    extends SearchReadApi[Game, Query.Game]:

  def search(query: Query.Game, from: From, size: Size): Fu[List[Game]] =
    client
      .search(query, from, size)
      .flatMap: res =>
        gameRepo.gamesFromSecondary(res.hitIds.map(id => GameId.apply(id.value)))

  def count(query: Query.Game) =
    client.count(query).dmap(_.count)

  def validateAccounts(query: Query.Game, forMod: Boolean): Fu[Boolean] =
    fuccess(forMod) >>| userApi.containsDisabled(query.userIds).not

  def idStream(query: Query.Game, total: Size, batchSize: MaxPerPage): Source[List[GameId], ?] =
    val pageSize = Size(batchSize.value.atMost(total.value))
    Source.unfoldAsync(0): from =>
      if from >= total.value then fuccess(none)
      else
        client
          .search(query, From(from), pageSize)
          .map: res =>
            Option.when(res.hitIds.nonEmpty && from < total.value):
              (from + pageSize.value) -> res.hitIds.map(id => GameId.apply(id.value))
