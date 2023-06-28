package lila.teamSearch

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.search.*
import lila.team.{ Team, TeamRepo }

final class TeamSearchApi(
    client: ESClient,
    teamRepo: TeamRepo
)(using
    ec: Executor,
    mat: akka.stream.Materializer
) extends SearchReadApi[Team, Query]:

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      teamRepo byOrderedIds TeamId.from(res.ids)
    }

  def count(query: Query) = client.count(query).dmap(_.value)

  def store(team: Team) = client.store(team.id into Id, toDoc(team))

  private def toDoc(team: Team) =
    Json.obj(
      Fields.name        -> team.name,
      Fields.description -> team.description.value.take(10000),
      Fields.nbMembers   -> team.nbMembers
    )

  def reset =
    client match
      case c: ESClientHttp =>
        c.putMapping >> {

          logger.info(s"Index to ${c.index}")

          teamRepo.cursor
            .documentSource()
            .via(lila.common.LilaStream.logRate[Team]("team index")(logger))
            .map(t => t.id.into(Id) -> toDoc(t))
            .grouped(200)
            .mapAsync(1)(c.storeBulk)
            .runWith(Sink.ignore)
        } >> client.refresh
      case _ => funit
