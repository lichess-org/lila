package lila.teamSearch

import akka.actor._
import akka.pattern.pipe
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mapping.FieldType._

import lila.search.actorApi._
import lila.team.actorApi._
import lila.team.Team

private[teamSearch] final class Indexer(
    client: ElasticClient,
    indexName: String,
    typeName: String) extends Actor {

  private val indexType = s"$indexName/$typeName"

  def receive = {

    case Search(definition) => client execute definition pipeTo sender
    case Count(definition)  => client execute definition pipeTo sender

    case InsertTeam(team) => client execute store(team)

    case RemoveTeam(id) => client execute {
      delete id id from indexType
    }

    case Reset =>
      lila.search.ElasticSearch.createType(client, indexName, typeName)
      try {
        client.putMapping(indexName) {
          typeName as (
            Fields.name typed StringType boost 3,
            Fields.description typed StringType boost 2,
            Fields.location typed StringType,
            Fields.nbMembers typed ShortType
          )
        }
        import scala.concurrent.Await
        import scala.concurrent.duration._
        import play.api.libs.json.Json
        import lila.db.api._
        import lila.team.tube.teamTube
        Await.result(
          $enumerate.bulk[Option[Team]]($query[Team](Json.obj("enabled" -> true)), 100) { teamOptions =>
            client bulk {
              (teamOptions.flatten map store): _*
            } void
          }, 20 minutes)
        sender ! (())
      }
      catch {
        case e: Exception =>
          println(e)
          sender ! Status.Failure(e)
      }
  }

  private def store(team: Team) =
    index into indexType fields {
      List(
        Fields.name -> team.name,
        Fields.description -> team.description.take(10000),
        Fields.location -> team.location,
        Fields.nbMembers -> team.nbMembers
      ): _*
    } id team.id
}
