package lila.simul

import org.joda.time.DateTime

import lila.user.User

private[simul] final class SimulCleaner(
    repo: SimulRepo,
    api: SimulApi,
    socketMap: SocketMap
) {

  def apply: Unit = repo.allCreated foreach { simuls =>
    simuls.map { simul =>
      socketMap.ask[Iterable[User.ID]](simul.id)(actorApi.GetUserIdsP.apply) map
        (_.toList contains simul.hostId) map {
          case true => repo setHostSeenNow simul
          case false if simul.hostSeenAt.??(_ isBefore DateTime.now.minusMinutes(3)) => api abort simul.id
          case false =>
        }
    }
  }
}
