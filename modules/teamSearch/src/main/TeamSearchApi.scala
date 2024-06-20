package lila.teamSearch

import akka.stream.scaladsl.*

import lila.core.team.TeamData
import lila.search.*
import lila.search.client.SearchClient
import lila.search.spec.{ Query, TeamSource }

final class TeamSearchApi(
    client: SearchClient,
    teamApi: lila.core.team.TeamApi
)(using Executor, akka.stream.Materializer)
    extends SearchReadApi[TeamId, Query.Team]:

  def search(query: Query.Team, from: From, size: Size) =
    client
      .search(query, from, size)
      .map: res =>
        res.hitIds.map(TeamId.apply)

  def count(query: Query.Team) = client.count(query).dmap(_.count)

  def store(team: TeamData) = client.storeTeam(team.id.value, toDoc(team))

  private def toDoc(team: TeamData) =
    TeamSource(
      name = team.name,
      description = team.description.value.take(10000),
      nbMembers = team.nbMembers
    )

  def reset =
    client.mapping(index) >> {

      logger.info(s"Index to ${index}")

      teamApi.cursor
        .documentSource()
        .via(lila.common.LilaStream.logRate[TeamData]("team index")(logger))
        .map(t => t.id.value -> toDoc(t))
        .grouped(200)
        .mapAsync(1)(xs => client.storeBulkTeam(xs.toList))
        .runWith(Sink.ignore)
    } >> client.refresh(index)
