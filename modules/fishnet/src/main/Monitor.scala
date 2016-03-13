package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.Implicits._

private final class Monitor(
    repo: FishnetRepo,
    scheduler: lila.common.Scheduler) {

  private def monitorClients: Unit = {

    import lila.mon.fishnet.client._
    import Client.Skill._

    repo.allClients foreach { clients =>

      status enabled clients.count(_.enabled)
      status disabled clients.count(_.disabled)

      skill move clients.count(_.skill == Move)
      skill analysis clients.count(_.skill == Analysis)
      skill all clients.count(_.skill == All)

      clients.flatMap(_.instance).map(_.version.value).groupBy(identity).mapValues(_.size) foreach {
        case (v, nb) => version(v)(nb)
      }
      clients.flatMap(_.instance).map(_.engine.name).groupBy(identity).mapValues(_.size) foreach {
        case (s, nb) => engine(s)(nb)
      }

      scheduleClients
    }
  }

  private def scheduleClients = scheduler.once(1 minute)(monitorClients)

  scheduleClients
}
