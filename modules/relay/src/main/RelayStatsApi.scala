package lila.relay

import lila.db.dsl.{ *, given }
import reactivemongo.api.bson.BSONInteger
import scalalib.cache.ExpireSetMemo

object RelayStats:
  type Minute = Int
  type Crowd  = Int
  type Graph  = List[(Minute, Crowd)]
  case class RoundStats(round: RelayRound, viewers: Graph)

final class RelayStatsApi(roundRepo: RelayRoundRepo, colls: RelayColls)(using scheduler: Scheduler)(using
    Executor
):
  import RelayStats.*
  import BSONHandlers.given

  // on measurement by minute at most; the storage depends on it.
  scheduler.scheduleWithFixedDelay(1 minute, 1 minute)(() => record())

  def get(id: RelayTourId): Fu[List[RoundStats]] =
    colls.round
      .aggregateList(RelayTour.maxRelays): framework =>
        import framework.*
        Match($doc("tourId" -> id)) -> List(
          Sort(Ascending("createdAt")),
          AddFields($doc("sync.log" -> $arr())),
          PipelineOperator(
            $lookup.simple(colls.stats, "stats", "_id", "_id")
          ),
          AddFields($doc("stats" -> $doc("$first" -> "$stats")))
        )
      .map: docs =>
        for
          doc   <- docs
          round <- doc.asOpt[RelayRound]
          data = for
            doc  <- doc.getAsOpt[Bdoc]("stats")
            data <- doc.getAsOpt[List[Int]]("d")
          yield data
          stats = data.so:
            _.grouped(2)
              .collect:
                case List(minute, crowd) => (minute, crowd)
              .toList
        yield RoundStats(round, stats)

  def setActive(id: RelayRoundId) = activeRounds.put(id)

  // keep monitoring rounds for some time after they stopped syncing
  private val activeRounds = ExpireSetMemo[RelayRoundId](1 hour)

  private def record(): Funit = for
    crowds <- fetchRoundCrowds
    nowMinutes = nowSeconds / 60
    lastValuesDocs <- colls.stats.aggregateList(crowds.size): framework =>
      import framework.*
      Match($inIds(crowds.map(_._1))) -> List(
        Project($doc("last" -> $doc("$arrayElemAt" -> $arr("$d", -1))))
      )
    lastValues = for
      doc  <- lastValuesDocs
      last <- doc.getAsOpt[Crowd]("last")
      id   <- doc.getAsOpt[RelayRoundId]("_id")
    yield (id, last)
    lastValuesMap = lastValues.toMap
    update        = colls.stats.update(ordered = false)
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
    val max = 500
    colls.round
      .aggregateList(maxDocs = max, _.sec): framework =>
        import framework.*
        Match(
          $doc(
            $or(
              $doc("sync.until" -> $exists(true)),
              $inIds(activeRounds.keys)
            ),
            "crowd".$gt(0)
          )
        ) ->
          List(Project($doc("_id" -> 1, "crowd" -> 1, "syncing" -> "$sync.until")))
      .map: docs =>
        if docs.size == max
        then logger.warn(s"RelayStats.fetchRoundCrowds: $max docs fetched")
        for
          doc   <- docs
          id    <- doc.getAsOpt[RelayRoundId]("_id")
          crowd <- doc.getAsOpt[Crowd]("crowd")
          _ = if doc.contains("syncing") then activeRounds.put(id)
        yield (id, crowd)
