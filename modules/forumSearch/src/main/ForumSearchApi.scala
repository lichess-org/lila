package lila.forumSearch

import lila.core.id.ForumPostId
import lila.search.*
import lila.search.client.SearchClient
import lila.search.spec.Query

final class ForumSearchApi(client: SearchClient)(using Executor)
    extends SearchReadApi[ForumPostId, Query.Forum]:

  def search(query: Query.Forum, from: From, size: Size) =
    client
      .search(query, from, size)
      .map(res => res.hitIds.map(id => ForumPostId.apply(id.value)))

  def count(query: Query.Forum) =
    client.count(query).dmap(_.count)
