package lila.forumSearch

import akka.stream.scaladsl.*

import lila.search.*
import lila.core.forum.{ ForumPostApi, ForumPostMini, ForumPostMiniView }
import lila.core.id.ForumPostId
import lila.search.client.SearchClient
import lila.search.spec.{ ForumSource, Query }

final class ForumSearchApi(
    client: SearchClient,
    postApi: ForumPostApi
)(using Executor, akka.stream.Materializer)
    extends SearchReadApi[ForumPostId, Query.Forum]:

  def search(query: Query.Forum, from: From, size: Size) =
    client
      .search(query, from.value, size.value)
      .map: res =>
        res.hitIds.map(ForumPostId.apply)

  def count(query: Query.Forum) =
    client.count(query).dmap(_.count)

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
      date = view.post.createdAt.toEpochMilli()
    )

  def reset =
    client.mapping(index) >>
      readAndIndexPosts(none) >>
      client.refresh(index)

  def backfill(since: Instant) =
    readAndIndexPosts(since.some)

  private def readAndIndexPosts(since: Option[Instant]) =
    postApi
      .nonGhostCursor(since)
      .documentSource()
      .via(lila.common.LilaStream.logRate("forum index")(logger))
      .grouped(200)
      .mapAsync(1)(posts => postApi.toMiniViews(posts.toList))
      .map(_.map(v => v.post.id.value -> toDoc(v)))
      .mapAsyncUnordered(2)(client.storeBulkForum)
      .runWith(Sink.ignore)
