package lila.teamSearch

import akka.actor._
import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._

import lila.search.{ actorApi => S }
import lila.team.actorApi._

private[teamSearch] final class Indexer(lowLevel: ActorRef) extends Actor {

  def receive = {

    case InsertTeam(team) => lowLevel ! S.InsertOne(team.id, Team from team)

    case RemoveTeam(id) => lowLevel ! S.RemoveOne(id)
  }
}
