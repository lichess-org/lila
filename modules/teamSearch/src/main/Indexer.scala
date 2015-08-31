package lila.teamSearch

import akka.actor._
import akka.pattern.pipe

import lila.search.ESClient
import lila.search.actorApi._
import lila.team.actorApi._
import lila.team.Team

private[teamSearch] final class Indexer(
    client: ESClient,
    indexName: String,
    typeName: String) extends Actor {

  private val indexType = s"$indexName/$typeName"

  def receive = {

//     case Search(definition) => client search definition pipeTo sender
//     case Count(definition)  => client count definition pipeTo sender

//     case InsertTeam(team)   => client store store(team)

//     case RemoveTeam(id) => client.deleteById(id, indexType)

    case Reset =>
      // client.createType(indexName, typeName)
      // try {
      //   client put {
      //     put mapping indexName/typeName as Seq(
      //       Fields.name typed StringType boost 3,
      //       Fields.description typed StringType boost 2,
      //       Fields.location typed StringType,
      //       Fields.nbMembers typed ShortType
      //     )
      //   }
      //   import scala.concurrent.Await
      //   import scala.concurrent.duration._
      //   import play.api.libs.json.Json
      //   import lila.db.api._
      //   import lila.team.tube.teamTube
      //   Await.result(
      //     $enumerate.bulk[Option[Team]]($query[Team](Json.obj("enabled" -> true)), 100) { teamOptions =>
      //       client bulk {
      //         bulk {
      //           (teamOptions.flatten map store): _*
      //         }
      //       }
      //     }, 20 minutes)
      //   sender ! (())
      // }
      // catch {
      //   case e: Exception =>
      //     println(e)
      //     sender ! Status.Failure(e)
      // }
  }

  // private def store(team: Team) =
  //   index into indexType fields {
  //     List(
  //       Fields.name -> team.name,
  //       Fields.description -> team.description.take(10000),
  //       Fields.location -> team.location,
  //       Fields.nbMembers -> team.nbMembers
  //     ): _*
  //   } id team.id
}
