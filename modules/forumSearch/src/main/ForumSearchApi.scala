package lila.forumSearch

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Json.given
import lila.search.*
import lila.core.forum.{ ForumPostApi, ForumPostMini, ForumPostMiniView }

final class ForumSearchApi(
    client: ESClient,
    postApi: ForumPostApi
)(using Executor, akka.stream.Materializer)
    extends SearchReadApi[ForumPostId, Query]:

  def search(query: Query, from: From, size: Size) =
    client
      .search(query, from, size)
      .map: res =>
        ForumPostId.from(res.ids)

  def count(query: Query) =
    client.count(query).dmap(_.value)

  def store(post: ForumPostMini) =
    postApi
      .toMiniView(post)
      .flatMapz: view =>
        client.store(view.post.id.into(Id), toDoc(view))

  private def toDoc(view: ForumPostMiniView) = Json.obj(
    Fields.body    -> view.post.text.take(10000),
    Fields.topic   -> view.topic.name,
    Fields.author  -> view.post.userId,
    Fields.topicId -> view.topic.id,
    Fields.troll   -> view.post.troll,
    Fields.date    -> view.post.createdAt
  )

  def reset =
    client match
      case c: ESClientHttp =>
        c.putMapping >> {
          postApi.nonGhostCursor
            .documentSource()
            .via(lila.common.LilaStream.logRate("forum index")(logger))
            .grouped(200)
            .mapAsync(1)(posts => postApi.toMiniViews(posts.toList))
            .map(_.map(v => v.post.id.into(Id) -> toDoc(v)))
            .mapAsyncUnordered(2)(c.storeBulk)
            .runWith(Sink.ignore)
        } >> client.refresh

      case _ => funit
