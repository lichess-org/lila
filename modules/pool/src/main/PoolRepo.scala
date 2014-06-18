package lila.pool

import akka.actor._
import akka.pattern.ask

import lila.hub.actorApi.map.{ Ask, AskAll }
import actorApi.GetPool
import makeTimeout.short
import scala.concurrent.duration._

final class PoolRepo(hub: ActorRef) {

  def byId(id: String): Fu[Option[Pool]] =
    hub ? Ask(id, GetPool) mapTo manifest[Pool] map (_.some) recover {
      case _: IllegalArgumentException => none
    }

  val all = lila.memo.AsyncCache.single(fetchAll, timeToLive = 10.seconds)

  private def fetchAll: Fu[List[Pool]] = hub ? AskAll(GetPool) mapTo manifest[List[Pool]] map { pools =>
    pools.sortBy(_.setup.id)
  }
}
