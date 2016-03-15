package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.Implicits._

private final class Monitor(
    repo: FishnetRepo,
    sequencer: Sequencer,
    scheduler: lila.common.Scheduler) {

  private[fishnet] def analysis(work: Work.Analysis, client: Client, result: JsonApi.Request.PostAnalysis) = {
    success(work, client)
    val monitor = lila.mon.fishnet.analysis by client.userId.value
    monitor move result.analysis.size
    result.engine.options.hashInt foreach { monitor.hash(_) }
    result.engine.options.threadsInt foreach { monitor.threads(_) }
    val nbSamples = (result.analysis.size / 8) min 10
    sample(result.analysis.filterNot(_.checkmate), nbSamples).foreach { move =>
      move.time foreach { monitor.movetime(_) }
      move.nodes foreach { monitor.node(_) }
      move.nps foreach { monitor.nps(_) }
      move.depth foreach { monitor.depth(_) }
      monitor pvSize move.pvList.size
    }
  }

  private[fishnet] def move(work: Work.Move, client: Client) = {
    success(work, client)
    if (work.level == 8) work.acquiredAt foreach { acquiredAt =>
      lila.mon.fishnet.move.time(client.userId.value)(nowMillis - acquiredAt.getMillis)
    }
  }

  private def sample[A](elems: List[A], n: Int) = scala.util.Random shuffle elems take n

  private def success(work: Work, client: Client) = {

    lila.mon.fishnet.client.result(client.userId.value, work.skill.key).success()

    work.acquiredAt foreach { acquiredAt =>
      lila.mon.fishnet.queue.db(work.skill.key) {
        acquiredAt.getMillis - work.createdAt.getMillis
      }
    }
  }

  private[fishnet] def failure(work: Work, client: Client) =
    lila.mon.fishnet.client.result(client.userId.value, work.skill.key).failure()

  private[fishnet] def timeout(work: Work, client: Client) =
    lila.mon.fishnet.client.result(client.userId.value, work.skill.key).timeout()

  private[fishnet] def abort(work: Work, client: Client) =
    lila.mon.fishnet.client.result(client.userId.value, work.skill.key).abort()

  private def monitorClients: Unit = repo.allRecentClients map { clients =>

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

  private def monitorWork: Unit = {

    import lila.mon.fishnet.work._
    import Client.Skill._

    sequencer.move.withQueueSize(lila.mon.fishnet.queue.sequencer(Move.key)(_))
    sequencer.analysis.withQueueSize(lila.mon.fishnet.queue.sequencer(Analysis.key)(_))

    repo.countMove(acquired = false).map { queued(Move.key)(_) } >>
      repo.countMove(acquired = true).map { acquired(Move.key)(_) } >>
      repo.countAnalysis(acquired = false).map { queued(Analysis.key)(_) } >>
      repo.countAnalysis(acquired = true).map { acquired(Analysis.key)(_) }

  } andThenAnyway scheduleWork

  private def scheduleClients = scheduler.once(1 minute)(monitorClients)
  private def scheduleWork = scheduler.once(10 seconds)(monitorWork)

  scheduleClients
  scheduleWork
}
