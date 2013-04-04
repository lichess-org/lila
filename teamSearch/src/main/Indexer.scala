package lila.teamSearch

import lila.team.actorApi._
import lila.search.{ actorApi ⇒ S }

import akka.actor._
import play.api.libs.concurrent.Execution.Implicits._
import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._

private[teamSearch] final class Indexer(lowLevel: ActorRef) extends Actor {

  def receive = {

    case InsertTeam(team) ⇒ lowLevel ! S.InsertOne(team.id, Team from team)

    case RemoveTeam(id) ⇒ lowLevel ! S.RemoveOne(id)
  }
}
