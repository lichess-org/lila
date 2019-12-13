package lila.fishnet

import scala.concurrent.duration._

final private class Monitor(
    repo: FishnetRepo
)(implicit system: akka.actor.ActorSystem) {

  private def sumOf[A](items: List[A])(f: A => Option[Int]) = items.foldLeft(0) {
    case (acc, a) => acc + f(a).getOrElse(0)
  }

  private[fishnet] def analysis(
      work: Work.Analysis,
      client: Client,
      result: JsonApi.Request.CompleteAnalysis
  ) = {
    Monitor.success(work, client)

    val monitor = lila.mon.fishnet.analysis by client.userId.value
    val threads = result.stockfish.options.threadsInt

    result.stockfish.options.hashInt foreach { monitor.hash.record(_) }
    result.stockfish.options.threadsInt foreach { monitor.threads.update(_) }

    monitor.totalSecond.increment(sumOf(result.evaluations)(_.time) * threads.|(1) / 1000)
    monitor.totalMeganode.increment(sumOf(result.evaluations) { eval =>
      eval.nodes ifFalse eval.mateFound
    } / 1000000)
    monitor.totalPosition.increment(result.evaluations.size)

    val metaMovesSample = sample(result.evaluations.drop(6).filterNot(_.mateFound), 100)
    def avgOf(f: JsonApi.Request.Evaluation => Option[Int]): Option[Int] = {
      val (sum, nb) = metaMovesSample.foldLeft(0 -> 0) {
        case ((sum, nb), move) =>
          f(move).fold(sum -> nb) { v =>
            (sum + v, nb + 1)
          }
      }
      (nb > 0) option (sum / nb)
    }
    avgOf(_.time) foreach { monitor.movetime.record(_) }
    avgOf(_.nodes) foreach { monitor.node.record(_) }
    avgOf(_.cappedNps) foreach { monitor.nps.record(_) }
    avgOf(_.depth) foreach { monitor.depth.record(_) }
    avgOf(_.pv.size.some) foreach { monitor.pvSize.record(_) }

    val significantPvSizes =
      result.evaluations.filterNot(_.mateFound).filterNot(_.deadDraw).map(_.pv.size)

    monitor.pv(false).increment(significantPvSizes.count(_ < 3))
    monitor.pv(true).increment(significantPvSizes.count(_ >= 6))
  }

  private def sample[A](elems: List[A], n: Int) =
    if (elems.size <= n) elems else scala.util.Random shuffle elems take n

  private def monitorClients(): Unit =
    repo.allRecentClients map { clients =>
      import lila.mon.fishnet.client._

      status(true).update(clients.count(_.enabled))
      status(false).update(clients.count(_.disabled))

      val instances = clients.flatMap(_.instance)

      instances.map(_.version.value).groupBy(identity).view.mapValues(_.size) foreach {
        case (v, nb) => version(v).update(nb)
      }
      instances.map(_.engines.stockfish.name).groupBy(identity).view.mapValues(_.size) foreach {
        case (s, nb) => stockfish(s).update(nb)
      }
      instances.map(_.python.value).groupBy(identity).view.mapValues(_.size) foreach {
        case (s, nb) => python(s).update(nb)
      }
    } addEffectAnyway scheduleClients

  private def monitorWork(): Unit = {

    import lila.mon.fishnet.work._

    repo.countAnalysis(acquired = false).map { queued.update(_) } >>
      repo.countAnalysis(acquired = true).map { acquired.update(_) } >>
      repo.countUserAnalysis.map { forUser.update(_) }

  } addEffectAnyway scheduleWork

  private def scheduleClients = system.scheduler.scheduleOnce(1 minute)(monitorClients)
  private def scheduleWork    = system.scheduler.scheduleOnce(20 seconds)(monitorWork)

  scheduleClients
  scheduleWork
}

object Monitor {

  private def success(work: Work, client: Client) = {

    lila.mon.fishnet.client.result(client.userId.value).success.increment()

    work.acquiredAt foreach { acquiredAt =>
      lila.mon.fishnet.queue.record {
        acquiredAt.getMillis - work.createdAt.getMillis
      }
    }
  }

  private[fishnet] def failure(work: Work, client: Client, e: Exception) = {
    logger.warn(s"Received invalid analysis ${work.id} for ${work.game.id} by ${client.fullId}", e)
    lila.mon.fishnet.client.result(client.userId.value).failure.increment()
  }

  private[fishnet] def weak(work: Work, client: Client, data: JsonApi.Request.CompleteAnalysis) = {
    logger.warn(
      s"Received weak analysis ${work.id} (nodes: ${~data.medianNodes}) for ${work.game.id} by ${client.fullId}"
    )
    lila.mon.fishnet.client.result(client.userId.value).weak.increment()
  }

  private[fishnet] def timeout(userId: Client.UserId) =
    lila.mon.fishnet.client.result(userId.value).timeout.increment()

  private[fishnet] def abort(client: Client) =
    lila.mon.fishnet.client.result(client.userId.value).abort.increment()

  private[fishnet] def notFound(id: Work.Id, client: Client) = {
    logger.info(s"Received unknown analysis $id by ${client.fullId}")
    lila.mon.fishnet.client.result(client.userId.value).notFound.increment()
  }

  private[fishnet] def notAcquired(work: Work, client: Client) = {
    logger.info(
      s"Received unacquired analysis ${work.id} for ${work.game.id} by ${client.fullId}. Work current tries: ${work.tries} acquired: ${work.acquired}"
    )
    lila.mon.fishnet.client.result(client.userId.value).notAcquired.increment()
  }
}
