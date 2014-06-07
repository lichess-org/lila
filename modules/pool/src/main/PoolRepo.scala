package lila.pool

import akka.actor._
import akka.pattern.ask

import lila.hub.actorApi.map.{ Ask, AskAll }
import actorApi.GetPool
import makeTimeout.short

final class PoolRepo(hub: ActorRef) {

  def byId(id: String): Fu[Option[Pool]] =
    hub ? Ask(id, GetPool) mapTo manifest[Pool] map (_.some) recover {
      case _: IllegalArgumentException => none
    }

  def all: Fu[List[Pool]] = hub ? AskAll(GetPool) mapTo manifest[List[Pool]]
}
