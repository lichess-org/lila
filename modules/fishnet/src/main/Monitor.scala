package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.Implicits._

private final class Monitor(
    repo: FishnetRepo,
    scheduler: lila.common.Scheduler) {

  private[fishnet] def success(client: Client, work: Work) = {

    lila.mon.fishnet.client.count(client.fullId, work.skill.key).success()

    work.acquiredAt foreach { acquiredAt =>

      lila.mon.fishnet.client.time(client.fullId, work.skill.key) {
        val totalTime = nowMillis - acquiredAt.getMillis
        work match {
          case a: Work.Analysis => totalTime / a.nbPly
          case m: Work.Move     => totalTime
        }
      }

      lila.mon.fishnet.queue.time(work.skill.key) {
        acquiredAt.getMillis - work.createdAt.getMillis
      }
    }
  }

  private[fishnet] def failure(client: Client, work: Work) =
    lila.mon.fishnet.client.count(client.fullId, work.skill.key).failure()

  private[fishnet] def timeout(client: Client, work: Work) =
    lila.mon.fishnet.client.count(client.fullId, work.skill.key).timeout()

  private def monitorClients: Unit = repo.allClients map { clients =>

    import lila.mon.fishnet.client._
    import Client.Skill._

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
  } andThenAnyway scheduleClients

  private def scheduleClients = scheduler.once(1 minute)(monitorClients)

  scheduleClients
}
