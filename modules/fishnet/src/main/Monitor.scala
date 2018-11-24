package lila.fishnet

import scala.concurrent.duration._

private final class Monitor(
    moveDb: MoveDB,
    repo: FishnetRepo,
    sequencer: lila.hub.FutureSequencer,
    scheduler: lila.common.Scheduler
) {

  private def sumOf[A](items: List[A])(f: A => Option[Int]) = items.foldLeft(0) {
    case (acc, a) => acc + f(a).getOrElse(0)
  }

  private[fishnet] def analysis(work: Work.Analysis, client: Client, result: JsonApi.Request.CompleteAnalysis) = {
    Monitor.success(work, client)

    val monitor = lila.mon.fishnet.analysis by client.userId.value
    val threads = result.stockfish.options.threadsInt

    result.stockfish.options.hashInt foreach { monitor.hash(_) }
    result.stockfish.options.threadsInt foreach { monitor.threads(_) }

    monitor.totalSecond(sumOf(result.evaluations)(_.time) * threads.|(1) / 1000)
    monitor.totalMeganode(sumOf(result.evaluations) { eval =>
      eval.nodes ifFalse eval.mateFound
    } / 1000000)
    monitor.totalPosition(result.evaluations.size)

    val metaMovesSample = sample(result.evaluations.drop(6).filterNot(_.mateFound), 100)
    def avgOf(f: JsonApi.Request.Evaluation => Option[Int]): Option[Int] = {
      val (sum, nb) = metaMovesSample.foldLeft(0 -> 0) {
        case ((sum, nb), move) => f(move).fold(sum -> nb) { v =>
          (sum + v, nb + 1)
        }
      }
      (nb > 0) option (sum / nb)
    }
    avgOf(_.time) foreach { monitor.movetime(_) }
    avgOf(_.nodes) foreach { monitor.node(_) }
    avgOf(_.cappedNps) foreach { monitor.nps(_) }
    avgOf(_.depth) foreach { monitor.depth(_) }
    avgOf(_.pv.size.some) foreach { monitor.pvSize(_) }

    val significantPvSizes =
      result.evaluations.filterNot(_.mateFound).filterNot(_.deadDraw).map(_.pv.size)

    monitor.pvTotal(significantPvSizes.size)
    monitor.pvShort(significantPvSizes.count(_ < 3))
    monitor.pvLong(significantPvSizes.count(_ >= 6))
  }

  private def sample[A](elems: List[A], n: Int) =
    if (elems.size <= n) elems else scala.util.Random shuffle elems take n

  private def monitorClients: Unit = repo.allRecentClients map { clients =>

    import lila.mon.fishnet.client._

    status enabled clients.count(_.enabled)
    status disabled clients.count(_.disabled)

    Client.Skill.all foreach { s =>
      skill(s.key)(clients.count(_.skill == s))
    }

    val instances = clients.flatMap(_.instance)

    instances.map(_.version.value).groupBy(identity).mapValues(_.size) foreach {
      case (v, nb) => version(v)(nb)
    }
    instances.map(_.engines.stockfish.name).groupBy(identity).mapValues(_.size) foreach {
      case (s, nb) => stockfish(s)(nb)
    }
    instances.map(_.python.value).groupBy(identity).mapValues(_.size) foreach {
      case (s, nb) => python(s)(nb)
    }
  } addEffectAnyway scheduleClients

  private def monitorWork: Unit = {

    import lila.mon.fishnet.work._
    import Client.Skill._

    moveDb.monitor

    lila.mon.fishnet.queue.sequencer(Analysis.key)(sequencer.queueSize)

    repo.countAnalysis(acquired = false).map { queued(Analysis.key)(_) } >>
      repo.countAnalysis(acquired = true).map { acquired(Analysis.key)(_) } >>
      repo.countUserAnalysis.map { forUser(Analysis.key)(_) }

  } addEffectAnyway scheduleWork

  private def scheduleClients = scheduler.once(1 minute)(monitorClients)
  private def scheduleWork = scheduler.once(10 seconds)(monitorWork)

  scheduleClients
  scheduleWork
}

object Monitor {

  private[fishnet] def move(work: Work.Move, client: Client) = {
    success(work, client)
    if (work.level == 8) work.acquiredAt foreach { acquiredAt =>
      lila.mon.fishnet.move.time(client.userId.value)(nowMillis - acquiredAt.getMillis)
    }
    if (work.level == 1)
      lila.mon.fishnet.move.fullTimeLvl1(client.userId.value)(nowMillis - work.createdAt.getMillis)
  }

  private def success(work: Work, client: Client) = {

    lila.mon.fishnet.client.result(client.userId.value, work.skill.key).success()

    work.acquiredAt foreach { acquiredAt =>
      lila.mon.fishnet.queue.db(work.skill.key) {
        acquiredAt.getMillis - work.createdAt.getMillis
      }
    }
  }

  private[fishnet] def failure(work: Work, client: Client, e: Exception) = {
    logger.warn(s"Received invalid ${work.skill} ${work.id} for ${work.game.id} by ${client.fullId}", e)
    lila.mon.fishnet.client.result(client.userId.value, work.skill.key).failure()
  }

  private[fishnet] def weak(work: Work, client: Client, data: JsonApi.Request.CompleteAnalysis) = {
    logger.warn(s"Received weak ${work.skill} ${work.id} (nodes: ${~data.medianNodes}) for ${work.game.id} by ${client.fullId}")
    lila.mon.fishnet.client.result(client.userId.value, work.skill.key).weak()
  }

  private[fishnet] def timeout(work: Work, userId: Client.UserId) =
    lila.mon.fishnet.client.result(userId.value, work.skill.key).timeout()

  private[fishnet] def abort(work: Work, client: Client) =
    lila.mon.fishnet.client.result(client.userId.value, work.skill.key).abort()

  private[fishnet] def notFound(id: Work.Id, client: Client) = {
    logger.info(s"Received unknown ${client.skill} $id by ${client.fullId}")
    lila.mon.fishnet.client.result(client.userId.value, client.skill.key).notFound()
  }

  private[fishnet] def notAcquired(work: Work, client: Client) = {
    logger.info(s"Received unacquired ${work.skill} ${work.id} for ${work.game.id} by ${client.fullId}. Work current tries: ${work.tries} acquired: ${work.acquired}")
    lila.mon.fishnet.client.result(client.userId.value, work.skill.key).notAcquired()
  }
}
