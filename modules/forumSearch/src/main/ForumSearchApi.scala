package lila.forumSearch

import akka.stream.scaladsl._
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import reactivemongo.api._

import lila.forum.{ Post, PostView, PostLiteView, PostApi, PostRepo }
import lila.search._

final class ForumSearchApi(
    client: ESClient,
    postApi: PostApi,
    postRepo: PostRepo
)(implicit mat: akka.stream.Materializer) extends SearchReadApi[PostView, Query] {

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

  def reset = client match {
    case c: ESClientHttp => c.putMapping >> {

      postRepo.cursor
        .documentSource()
        .grouped(500)
        .mapAsyncUnordered(1)(postApi.liteViews)
        .map(views => views.map(v => Id(v.post.id) -> toDoc(v)))
        .mapAsyncUnordered(1) { views =>
          c.storeBulk(views) inject views.size
        }
        .fold(0)((acc, nb) => acc + nb)
        .wireTap { nb =>
          if (nb % 5 == 0) logger.info(s"Indexing forum posts... $nb")
        }
        .to(Sink.ignore)
        .run
    } >> client.refresh

    case _ => funit
  }
}
