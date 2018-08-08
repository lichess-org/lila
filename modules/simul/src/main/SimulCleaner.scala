package lidraughts.simul

import akka.actor._
import akka.pattern.ask
import lidraughts.hub.actorApi.map.Ask
import makeTimeout.short
import org.joda.time.DateTime

private[simul] final class SimulCleaner(
    repo: SimulRepo,
    api: SimulApi,
    socketHub: ActorRef
) {

  def apply: Unit = {
    repo.allCreated foreach { simuls =>
      simuls.map { simul =>
        socketHub ? Ask(simul.id, Socket.GetUserIds) mapTo
          manifest[Iterable[String]] map
          (_.toList contains simul.hostId) map {
            case true => repo setHostSeenNow simul
            case false if simul.hostSeenAt.??(_ isBefore DateTime.now.minusMinutes(3)) => api abort simul.id
            case false =>
          }
      }
    }
  }
}
