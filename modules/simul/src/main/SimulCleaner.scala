package lila.simul

import akka.pattern.ask
import lila.hub.actorApi.map.Ask
import makeTimeout.short
import org.joda.time.DateTime

private[simul] final class SimulCleaner(
    repo: SimulRepo,
    api: SimulApi,
    socketHub: lila.hub.ActorMapNew
) {

  def apply: Unit = {
    repo.allCreated foreach { simuls =>
      simuls.map { simul =>
        socketHub.ask[Iterable[String]](simul.id, Socket.GetUserIds) map
          (_.toList contains simul.hostId) map {
            case true => repo setHostSeenNow simul
            case false if simul.hostSeenAt.??(_ isBefore DateTime.now.minusMinutes(3)) => api abort simul.id
            case false =>
          }
      }
    }
  }
}
