package lila.simul

import org.joda.time.DateTime

final private[simul] class SimulCleaner(
    repo: SimulRepo,
    api: SimulApi
) {

  def cleanUp: Funit = repo.allCreated.map {
    _ foreach { simul =>
      val minutesAgo = DateTime.now.minusMinutes(2)
      if (simul.createdAt.isBefore(minutesAgo) &&
          !simul.hostSeenAt.exists(_ isAfter minutesAgo)) api.abort(simul.id)
    }
  }

  def hostPing(simul: Simul) = repo setHostSeenNow simul
}
