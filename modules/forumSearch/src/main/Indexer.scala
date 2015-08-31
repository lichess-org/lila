package lila.forumSearch

import akka.actor._
import akka.pattern.pipe

import lila.forum.actorApi._
import lila.forum.{ Post, PostLiteView, PostApi }
import lila.search.ESClient
import lila.search.actorApi._

private[forumSearch] final class Indexer(
    client: ESClient,
    indexName: String,
    typeName: String,
    postApi: PostApi) extends Actor {

  private val indexType = s"$indexName/$typeName"

  def receive = {

//     case Search(definition) => client search definition pipeTo sender
//     case Count(definition)  => client count definition pipeTo sender

//     case InsertPost(post) => postApi liteView post foreach {
//       _ foreach { view =>
//         client store store(view)
//       }
//     }

//     case RemovePost(id) => client.deleteById(id, indexType)

//     case RemoveTopic(id) => client.deleteByQuery(s"${Fields.topicId}:$id", indexType)

    case Reset =>
//       client.createType(indexName, typeName)
//       try {
//         client put {
//           put mapping indexName / typeName as Seq(
//             Fields.body typed StringType boost 2,
//             Fields.topic typed StringType boost 4,
//             Fields.author typed StringType index "not_analyzed",
//             Fields.topicId typed StringType,
//             Fields.staff typed BooleanType,
//             Fields.troll typed BooleanType,
//             Fields.date typed DateType
//           )
//         }
//         import scala.concurrent.Await
//         import scala.concurrent.duration._
//         import play.api.libs.json.Json
//         import lila.db.api._
//         import lila.forum.tube.postTube
//         Await.result(
//           $enumerate.bulk[Option[Post]]($query[Post](Json.obj()), 200) { postOptions =>
//             (postApi liteViews postOptions.flatten) flatMap { views =>
//               client bulk {
//                 bulk {
//                   (views map store): _*
//                 }
//               }
//             }
//           }, 20 minutes)
//         sender ! (())
//       }
//       catch {
//         case e: Exception =>
//           println(e)
//           sender ! Status.Failure(e)
//       }
  }

  // private def store(view: PostLiteView) =
  //   index into indexType fields {
  //     List(
  //       Fields.body -> view.post.text.take(10000),
  //       Fields.topic -> view.topic.name,
  //       Fields.author -> ~(view.post.userId orElse view.post.author map (_.toLowerCase)),
  //       Fields.topicId -> view.topic.id,
  //       Fields.staff -> view.post.isStaff,
  //       Fields.troll -> view.post.troll,
  //       Fields.date -> view.post.createdAt.getDate
  //     ): _*
  //   } id view.post.id
}
