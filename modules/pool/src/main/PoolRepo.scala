package lila.pool

import akka.actor._
import akka.pattern.ask

import actorApi.GetPool
import lila.hub.actorApi.map.{ Ask, AskAll }
import makeTimeout.short
import scala.concurrent.duration._

final class PoolRepo(hub: ActorRef, setupRepo: PoolSetupRepo) {

  def byId(id: String): Fu[Option[Pool]] = (setupRepo exists id) ?? {
    hub ? Ask(id, GetPool) mapTo manifest[Pool] map (_.some) recover {
      case _: IllegalArgumentException => none
    }
  }

  val all = lila.memo.AsyncCache.single(fetchAll, timeToLive = 5.seconds)

  private def fetchAll: Fu[List[Pool]] = hub ? AskAll(GetPool) mapTo manifest[List[Pool]] map { pools =>
    pools.sortBy(_.setup.id)
  }
}
