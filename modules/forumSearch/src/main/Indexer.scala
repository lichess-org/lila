package lila.forumSearch

import akka.actor._
import akka.pattern.pipe
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mapping.FieldType._
import scala.concurrent.Await
import scala.concurrent.duration._

import lila.forum.actorApi._
import lila.forum.{ Post, PostLiteView, PostApi }
import lila.search.actorApi._

private[forumSearch] final class Indexer(
    client: ElasticClient,
    indexName: String,
    typeName: String,
    postApi: PostApi) extends Actor {

  private val indexType = s"$indexName/$typeName"

  def receive = {

    case Search(definition) => client execute definition pipeTo sender

    case Count(definition)  => client execute definition pipeTo sender

    case InsertPost(post) => postApi liteView post foreach {
      _ foreach { view =>
        client execute {
          indexInto(indexType, view)
        }
      }
    }

    case RemovePost(id) => client execute {
      delete id id from indexType
    }

    case RemoveTopic(id) => client execute {
      delete from indexType where s"${Fields.topicId}:$id"
    }

    case Indexer.Reset =>
      lila.search.ElasticSearch.createType(client, indexName, typeName)
      try {
        client.putMapping(indexName) {
          typeName as (
            Fields.body typed StringType boost 2,
            Fields.topic typed StringType boost 4,
            Fields.author typed StringType,
            Fields.topicId typed StringType,
            Fields.staff typed BooleanType,
            Fields.troll typed BooleanType,
            Fields.date typed DateType
          )
        }
        import play.api.libs.json._
        import play.api.libs.iteratee._
        import lila.db.api._
        import lila.forum.tube.postTube
        Await.result(
          $enumerate.bulk[Option[Post]]($query[Post](Json.obj()), 1000) { postOptions =>
            (postApi liteViews postOptions.flatten) map { views =>
              client bulk {
                views.map { indexInto(indexType, _) }: _*
              }
            }
          }, 20 minutes)
        sender ! ()
      }
      catch {
        case e: Exception =>
          println(e)
          sender ! Status.Failure(e)
      }
  }

  def indexInto(indexType: String, view: PostLiteView) =
    index into indexType fields {
      List(
        Fields.body -> view.post.text.take(10000),
        Fields.topic -> view.topic.name,
        Fields.author -> ~(view.post.userId orElse view.post.author map (_.toLowerCase)),
        Fields.topicId -> view.topic.id,
        Fields.staff -> view.post.isStaff,
        Fields.troll -> view.post.troll,
        Fields.date -> view.post.createdAt.getDate
      ): _*
    } id view.post.id
}

private[forumSearch] object Indexer {

  case object Reset
}
