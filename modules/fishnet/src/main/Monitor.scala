package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.Implicits._

private final class Monitor(
    repo: FishnetRepo,
    scheduler: lila.common.Scheduler) {

  private[fishnet] def analysis(client: Client, work: Work, result: JsonApi.Request.PostAnalysis) = {
    success(client, work)
    val monitor = lila.mon.fishnet.analysis by client.userId.value
    monitor move result.analysis.size
    sample(result.analysis.filterNot(_.checkmate), 20).foreach { move =>
      monitor movetime move.time
      monitor node move.nodes
      monitor nps move.nps
      monitor depth move.depth
      monitor pvSize move.pvList.size
    }
  }

  private[fishnet] def move(client: Client, work: Work) = success(client, work)

  private def sample[A](elems: List[A], n: Int) = scala.util.Random shuffle elems take n

  private def success(client: Client, work: Work) = {

    lila.mon.fishnet.client.result(client.userId.value, work.skill.key).success()

    work.acquiredAt foreach { acquiredAt =>

      lila.mon.fishnet.client.time(client.userId.value, work.skill.key) {
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
    lila.mon.fishnet.client.result(client.userId.value, work.skill.key).failure()

  private[fishnet] def timeout(client: Client, work: Work) =
    lila.mon.fishnet.client.result(client.userId.value, work.skill.key).timeout()

  private def monitorClients: Unit = repo.allClients map { clients =>

    import lila.mon.fishnet.client._

    status enabled clients.count(_.enabled)
    status disabled clients.count(_.disabled)

    Client.Skill.all foreach { s =>
      skill(s.key)(clients.count(_.skill == s))
    }

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
