package lila.forumSearch

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Json.given
import lila.search.*
import lila.core.forum.{ ForumPostApi, ForumPostMini, ForumPostMiniView }
import lila.core.id.ForumPostId
import lila.search.client.SearchClient
import lila.search.spec.Query as SQuery
import lila.search.spec.ForumSource
import smithy4s.Timestamp

final class ForumSearchApi(
    client: SearchClient,
    postApi: ForumPostApi
)(using Executor, akka.stream.Materializer)
    extends SearchReadApi[ForumPostId, Query]:

  def search(query: Query, from: From, size: Size) =
    client
      .search(query.transform, from.value, size.value)
      .map: res =>
        res.hitIds.map(ForumPostId.apply)

  def count(query: Query) =
    client.count(query.transform).dmap(_.count)

  def store(post: ForumPostMini) =
    postApi
      .toMiniView(post)
      .flatMapz: view =>
        client.storeForum(view.post.id.value, toDoc(view))

  private def toDoc(view: ForumPostMiniView) =
    ForumSource(
      body = view.post.text.take(10000),
      topic = view.topic.name,
      author = view.post.userId.map(_.value),
      topicId = view.topic.id.value,
      troll = view.post.troll,
      date = lila.search.spec.SearchDateTime.fromInstant(view.post.createdAt)
    )

  def reset =
    client.mapping(index) >> {
      postApi.nonGhostCursor
        .documentSource()
        .via(lila.common.LilaStream.logRate("forum index")(logger))
        .grouped(200)
        .mapAsync(1)(posts => postApi.toMiniViews(posts.toList))
        .map(_.map(v => v.post.id.value -> toDoc(v)))
        .mapAsyncUnordered(2)(client.storeBulkForum)
        .runWith(Sink.ignore)
    } >> client.refresh(index)
