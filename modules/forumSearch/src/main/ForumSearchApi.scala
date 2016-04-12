package lila.forumSearch

import lila.forum.actorApi._
import lila.forum.{ Post, PostView, PostLiteView, PostApi, PostRepo }
import lila.search._

import play.api.libs.json._

final class ForumSearchApi(
    client: ESClient,
    postApi: PostApi) extends SearchReadApi[PostView, Query] {

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      postApi.viewsFromIds(res.ids)
    }

  def count(query: Query) =
    client.count(query) map (_.count)

  def store(post: Post) = postApi liteView post flatMap {
    _ ?? { view =>
      client.store(Id(view.post.id), toDoc(view))
    }
  }

  private def toDoc(view: PostLiteView) = Json.obj(
    Fields.body -> view.post.text.take(10000),
    Fields.topic -> view.topic.name,
    Fields.author -> ~(view.post.userId orElse view.post.author map (_.toLowerCase)),
    Fields.topicId -> view.topic.id,
    Fields.staff -> view.post.isStaff,
    Fields.troll -> view.post.troll,
    Fields.date -> view.post.createdAt.getDate)

  def reset = client match {
    case c: ESClientHttp => c.putMapping >> {
      lila.log("forumSearch").info(s"Index to ${c.index.name}")
      import lila.db.dsl._
      import play.api.libs.iteratee._
      PostRepo.cursor($empty).enumerateBulks(Int.MaxValue) |>>>
        Iteratee.foldM[Iterator[Post], Unit](()) {
          case (_, posts) => (postApi liteViews posts.toList) flatMap { views =>
            c.storeBulk(views map (v => Id(v.post.id) -> toDoc(v)))
          }
        }
    }
    case _ => funit
  }
}
