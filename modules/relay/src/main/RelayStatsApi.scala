package lila.relay

import lila.db.dsl.{ *, given }

private object RelayStats:
  type Minute = Int
  type Crowd = Int
  type Graph = List[(Minute, Crowd)]
  case class RoundStats(viewers: Graph)

final class RelayStatsApi(colls: RelayColls, viewerCount: lila.memo.ViewerCountApi)(using
    scheduler: Scheduler
)(using Executor):
  import RelayStats.*

  object viewers:

    def hit(rt: RelayRound.WithTour)(using ctx: lila.ui.Context): Unit =
      maxCountIfRecent(rt).foreach: maxCount =>
        viewerCount.hit(rt.round.id.value, maxCount)(ctx.req, ctx.userId)

    def get(rt: RelayRound.WithTour): Fu[Int] =
      rt.tour.official.so(viewerCount.get(rt.round.id.value))

    private def maxCountIfRecent(rt: RelayRound.WithTour) =
      rt.tour.tier
        .map:
          case RelayTour.Tier.normal => 1_000
          case RelayTour.Tier.high => 10_000
          case RelayTour.Tier.best => 100_000
        .ifTrue(rt.tour.daysSinceFinished.forall(_ <= 1))
        .ifTrue(rt.round.daysSinceFinished.forall(_ <= 3))
        .ifTrue(rt.round.hasStarted || rt.round.startsSoonOrAfterPrevious)

  // one measurement by minute at most; the storage depends on it.
  scheduler.scheduleWithFixedDelay(2.minutes, 2.minutes)(() => record())

  private def get(id: RelayRoundId): Fu[RoundStats] =
    colls.stats
      .primitiveOne[List[Int]]($id(id), "d")
      .mapz:
        _.grouped(2)
          .collect:
            case List(minute, crowd) => (minute, crowd)
          .toList
      .map(RoundStats.apply)

  def getJson(rt: RelayRound.WithTour) = for
    stats <- get(rt.round.id)
    unique <- viewers.get(rt)
  yield RelayJsonView.statsJson(stats, unique)

  private def record(): Funit = for
    crowds <- fetchRoundCrowds
    nowMinutes = nowSeconds / 60
    lastValuesDocs <- colls.stats.aggregateList(crowds.size): framework =>
      import framework.*
      Match($inIds(crowds._1F)) -> List(
        Project($doc("last" -> $doc("$arrayElemAt" -> $arr("$d", -1))))
      )
    lastValues =
      for
        doc <- lastValuesDocs
        last <- doc.getAsOpt[Crowd]("last")
        id <- doc.getAsOpt[RelayRoundId]("_id")
      yield (id, last)
    lastValuesMap = lastValues.toMap
    update = colls.stats.update(ordered = false)
    elementOpts <- crowds.sequentially: (roundId, crowd) =>
      val lastValue = ~lastValuesMap.get(roundId)
      (lastValue != crowd).so:
        update
          .element(
            q = $id(roundId),
            u = $push("d" -> $doc("$each" -> $arr(nowMinutes, crowd))),
            upsert = true
          )
          .dmap(some)
    elements = elementOpts.flatten
    _ <- elements.nonEmpty.so(update.many(elements).void)
  yield ()

  private def fetchRoundCrowds: Fu[List[(RelayRoundId, Crowd)]] =
    val max = 200
    colls.round
      .aggregateList(maxDocs = max, _.sec): framework =>
        import framework.*
        // lila-ws sets crowdAt along with crowd
        // so we can use crowdAt to know which rounds are being monitored
        Match($doc("crowdAt".$gt(nowInstant.minusMinutes(1)))) ->
          List(Project($doc("_id" -> 1, "crowd" -> 1)))
      .map: docs =>
        lila.mon.relay.crowdMonitor.update(docs.size)
        if docs.size == max
        then logger.warn(s"RelayStats.fetchRoundCrowds: $max docs fetched")
        for
          doc <- docs
          id <- doc.getAsOpt[RelayRoundId]("_id")
          crowd <- doc.getAsOpt[Crowd]("crowd")
        yield (id, crowd)
