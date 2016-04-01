package lila.teamSearch

import lila.search._
import lila.team.actorApi._
import lila.team.Team

import play.api.libs.json._

final class TeamSearchApi(
    client: ESClient,
    fetcher: Seq[String] => Fu[List[Team]]) extends SearchReadApi[Team, Query] {

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      fetcher(res.ids)
    }

  def count(query: Query) = client.count(query) map (_.count)

  def store(team: Team) = client.store(Id(team.id), toDoc(team))

  private def toDoc(team: Team) = Json.obj(
    Fields.name -> team.name,
    Fields.description -> team.description.take(10000),
    Fields.location -> team.location,
    Fields.nbMembers -> team.nbMembers)

  def reset = client match {
    case c: ESClientHttp => c.putMapping >> {
      lila.log("teamSearch").info(s"Index to ${c.index.name}")
      import lila.db.dsl._
      import lila.team.tube.teamTube
      $enumerate.bulk[Option[Team]]($query[Team](Json.obj("enabled" -> true)), 300) { teamOptions =>
        c.storeBulk(teamOptions.flatten map (t => Id(t.id) -> toDoc(t)))
      }
    }
    case _ => funit
  }
}
