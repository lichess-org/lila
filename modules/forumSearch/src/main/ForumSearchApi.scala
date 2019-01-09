package lila.forumSearch

import lila.forum.{ Post, PostView, PostLiteView, PostApi, PostRepo }
import lila.search._

import play.api.libs.json._

final class ForumSearchApi(
    client: ESClient,
    postApi: PostApi
) extends SearchReadApi[PostView, Query] {

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
    Fields.troll -> view.post.troll,
    Fields.date -> view.post.createdAt
  )

  import reactivemongo.play.iteratees.cursorProducer

  def reset = client match {
    case c: ESClientHttp => c.putMapping >> {
      import play.api.libs.iteratee._
      import reactivemongo.api.ReadPreference
      import lila.db.dsl._
      logger.info(s"Index to ${c.index.name}")
      val batchSize = 500
      val maxEntries = Int.MaxValue
      PostRepo.cursor(
        selector = $empty,
        readPreference = ReadPreference.secondaryPreferred
      )
        .enumerator(maxEntries) &>
        Enumeratee.grouped(Iteratee takeUpTo batchSize) |>>>
        Iteratee.foldM[Seq[Post], Int](0) {
          case (nb, posts) => for {
            views <- postApi liteViews posts.toList
            _ <- c.storeBulk(views map (v => Id(v.post.id) -> toDoc(v)))
          } yield {
            logger.info(s"Indexed $nb forum posts")
            nb + posts.size
          }
        }
    } >> client.refresh

    case _ => funit
  }
}
