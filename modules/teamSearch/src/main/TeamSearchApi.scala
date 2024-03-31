package lila.teamSearch

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.search.*
import lila.core.team.TeamData

final class TeamSearchApi(
    client: ESClient,
    teamApi: lila.core.team.TeamApi
)(using Executor, akka.stream.Materializer)
    extends SearchReadApi[TeamId, Query]:

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size).map { res =>
      TeamId.from(res.ids)
    }

  def count(query: Query) = client.count(query).dmap(_.value)

  def store(team: TeamData) = client.store(team.id.into(Id), toDoc(team))

  private def toDoc(team: TeamData) =
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

          teamApi.cursor
            .documentSource()
            .via(lila.common.LilaStream.logRate[TeamData]("team index")(logger))
            .map(t => t.id.into(Id) -> toDoc(t))
            .grouped(200)
            .mapAsync(1)(c.storeBulk)
            .runWith(Sink.ignore)
        } >> client.refresh
      case _ => funit
