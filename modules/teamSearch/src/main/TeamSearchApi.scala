package lila.teamSearch

import akka.stream.scaladsl._
import play.api.libs.json._
import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import reactivemongo.api._

import lila.db.dsl._
import lila.search._
import lila.team.{ Team, TeamRepo }

final class TeamSearchApi(
    client: ESClient,
    teamRepo: TeamRepo
)(implicit mat: akka.stream.Materializer) extends SearchReadApi[Team, Query] {

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      teamRepo byOrderedIds res.ids
    }

  def count(query: Query) = client.count(query) map (_.count)

  def store(team: Team) = client.store(Id(team.id), toDoc(team))

  private def toDoc(team: Team) = Json.obj(
    Fields.name -> team.name,
    Fields.description -> team.description.take(10000),
    Fields.location -> team.location,
    Fields.nbMembers -> team.nbMembers
  )

  def reset = client match {
    case c: ESClientHttp => c.putMapping >> {

      logger.info(s"Index to ${c.index.name}")

      teamRepo.cursor
        .documentSource()
        .via(lila.common.LilaStream.logRate[Team]("team index")(logger))
        .map(t => Id(t.id) -> toDoc(t))
        .grouped(200)
        .mapAsyncUnordered(1) { teams =>
          c.storeBulk(teams) inject teams.size
        }
        .fold(0)((acc, nb) => acc + nb)
        .wireTap { nb =>
          if (nb % 5 == 0) logger.info(s"Indexing teams... $nb")
        }
        .to(Sink.ignore)
        .run
    } >> client.refresh
    case _ => funit
  }
}
