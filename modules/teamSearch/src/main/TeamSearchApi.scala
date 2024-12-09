package lila.teamSearch

import lila.search.*
import lila.search.client.SearchClient
import lila.search.spec.Query

final class TeamSearchApi(client: SearchClient)(using Executor) extends SearchReadApi[TeamId, Query.Team]:

  def search(query: Query.Team, from: From, size: Size) =
    client
      .search(query, from, size)
      .map(_.hitIds.map(id => TeamId.apply(id.value)))

  def count(query: Query.Team) = client.count(query).dmap(_.count)
