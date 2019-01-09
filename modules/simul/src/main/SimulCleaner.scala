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
      socketMap.askIfPresent[Iterable[User.ID]](simul.id)(actorApi.GetUserIdsP) map { users =>
        users.??(_.toList contains simul.hostId) match {
          case true => repo setHostSeenNow simul
          case false if simul.hostSeenAt.??(_ isBefore DateTime.now.minusMinutes(3)) => api abort simul.id
          case false =>
        }
      }
    }
  }
}
