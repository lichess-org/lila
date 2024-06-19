package lila.forumSearch

import lila.search.*
import lila.core.id.ForumPostId
import lila.search.client.SearchClient
import lila.search.spec.{ ForumSource, Query }

final class ForumSearchApi(client: SearchClient)(using Executor)
    extends SearchReadApi[ForumPostId, Query.Forum]:

  def search(query: Query.Forum, from: From, size: Size) =
    client
      .search(query, from.value, size.value)
      .map(res => res.hitIds.map(ForumPostId.apply))

  def count(query: Query.Forum) =
    client.count(query).dmap(_.count)
