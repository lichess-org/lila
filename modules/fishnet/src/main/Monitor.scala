package lila.fishnet

import scalalib.ThreadLocalRandom

import lila.core.chess.Depth

final private class Monitor(
    repo: FishnetRepo,
    cacheApi: lila.memo.CacheApi
)(using ec: Executor, scheduler: Scheduler):

  val statusCache = cacheApi.unit[Monitor.Status]:
    _.refreshAfterWrite(1.minute).buildAsyncFuture: _ =>
      repo.status.compute

  private val monBy = lila.mon.fishnet.analysis.by

  private def sumOf[A](items: List[A])(f: A => Option[Int]) =
    items.foldLeft(0) { (acc, a) =>
      acc + f(a).getOrElse(0)
    }

  private[fishnet] def analysis(
      work: Work.Analysis,
      client: Client,
      result: JsonApi.Request.CompleteAnalysis
  ) =
    Monitor.success(work, client)

    import client.userId

    monBy.totalSecond(userId).increment(sumOf(result.evaluations)(_.time) / 1000)

    if result.stockfish.isNnue then
      monBy
        .totalMeganode(userId)
        .increment(sumOf(result.evaluations) { eval =>
          eval.nodes.ifFalse(eval.mateFound)
        } / 1000000)

    val metaMovesSample = sample(result.evaluations.drop(6).filterNot(_.mateFound), 100)
    def avgOf(f: JsonApi.Request.Evaluation => Option[Int]): Option[Int] =
      val (sum, nb) = metaMovesSample.foldLeft(0 -> 0) { case ((sum, nb), move) =>
        f(move).fold(sum -> nb) { v =>
          (sum + v, nb + 1)
        }
      }
      (nb > 0).option(sum / nb)
    avgOf(_.time).foreach { monBy.movetime(userId).record(_) }
    if result.stockfish.isNnue then
      avgOf(_.nodes).foreach { monBy.node(userId).record(_) }
      avgOf(_.cappedNps).foreach { monBy.nps(userId).record(_) }
    avgOf(e => Depth.raw(e.depth)).foreach { monBy.depth(userId).record(_) }
    avgOf(_.pv.size.some).foreach { monBy.pvSize(userId).record(_) }

    val significantPvSizes =
      result.evaluations.withFilter(e => !(e.mateFound || e.deadDraw)).map(_.pv.size)

    monBy.pv(userId, isLong = false).increment(significantPvSizes.count(_ < 3))
    monBy.pv(userId, isLong = true).increment(significantPvSizes.count(_ >= 6))

  private def sample[A](elems: List[A], n: Int) =
    if elems.sizeIs <= n then elems else ThreadLocalRandom.shuffle(elems).take(n)

  private def monitorClients(): Funit =
    repo.allRecentClients.map { clients =>
      import lila.mon.fishnet.client.*

      status(true).update(clients.count(_.enabled))
      status(false).update(clients.count(_.disabled))

      val instances = clients.flatMap(_.instance)

      instances.groupMapReduce(_.version.value)(_ => 1)(_ + _).foreach { (v, nb) =>
        version(v).update(nb)
      }
    }

  private def monitorStatus(): Funit =
    statusCache.get {}.map { c =>
      lila.mon.fishnet.work("queued", "system").update(c.system.queued)
      lila.mon.fishnet.work("queued", "user").update(c.user.queued)
      lila.mon.fishnet.work("acquired", "system").update(c.system.acquired)
      lila.mon.fishnet.work("acquired", "user").update(c.user.acquired)
      lila.mon.fishnet.oldest("system").update(c.system.oldest)
      lila.mon.fishnet.oldest("user").update(c.user.oldest)
      ()
    }

  scheduler.scheduleWithFixedDelay(1.minute, 1.minute) { () =>
    monitorClients() >> monitorStatus()
    ()
  }

object Monitor:

  case class StatusFor(acquired: Int, queued: Int, oldest: Int)
  case class Status(user: StatusFor, system: StatusFor):
    lazy val json: JsonStr =
      import play.api.libs.json.Json
      def statusFor(s: Monitor.StatusFor) =
        Json.obj(
          "acquired" -> s.acquired,
          "queued" -> s.queued,
          "oldest" -> s.oldest
        )
      JsonStr(
        Json.stringify(
          Json.obj(
            "analysis" -> Json.obj(
              "user" -> statusFor(user),
              "system" -> statusFor(system)
            )
          )
        )
      )

  private val monResult = lila.mon.fishnet.client.result

  private def success(work: Work.Analysis, client: Client) =

    monResult.success(client.userId).increment()

    work.acquiredAt.foreach { acquiredAt =>
      lila.mon.fishnet
        .queueTime(if work.sender.system then "system" else "user")
        .record:
          acquiredAt.toMillis - work.createdAt.toMillis
    }

  private[fishnet] def failure(work: Work, client: Client, e: Exception) =
    logger.warn(s"Received invalid analysis ${work.id} for ${work.game.id} by ${client.fullId}", e)
    monResult.failure(client.userId).increment()

  private[fishnet] def timeout(userId: UserId) =
    monResult.timeout(userId).increment()

  private[fishnet] def abort(client: Client) =
    monResult.abort(client.userId).increment()

  private[fishnet] def notFound(id: Work.Id, client: Client) =
    logger.info(s"Received unknown analysis $id by ${client.fullId}")
    monResult.notFound(client.userId).increment()

  private[fishnet] def notAcquired(work: Work, client: Client) =
    logger.info(
      s"Received unacquired analysis ${work.id} for ${work.game.id} by ${client.fullId}. Work current tries: ${work.tries} acquired: ${work.acquired}"
    )
    monResult.notAcquired(client.userId).increment()
