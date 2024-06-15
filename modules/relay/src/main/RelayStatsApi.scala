package lila.relay

import lila.db.dsl.{ *, given }

object RelayStats:
  type Minute = Int
  type Crowd  = Int
  type Graph  = List[(Minute, Crowd)]

final class RelayStatsApi(roundRepo: RelayRoundRepo, colls: RelayColls)(using scheduler: Scheduler)(using
    Executor
):
  import RelayStats.*

  // on measurement by minute at most; the storage depends on it.
  scheduler.scheduleWithFixedDelay(0 seconds, 1 minute)(() => record())

  def get(id: RelayTourId): Fu[Graph] =
    colls.stats
      .primitiveOne[List[Int]]($id(id), "d")
      .mapz:
        _.grouped(2)
          .collect:
            case List(minute, crowd) => (minute, crowd)
          .toList

  private def record(): Funit = for
    crowds <- roundRepo.tourCrowds
    nowMinutes = nowSeconds / 60
    update     = colls.stats.update(ordered = false)
    elements <- crowds.sequentially: (tourId, crowd) =>
      update.element(
        q = $id(tourId),
        u = $push("d" -> $doc("$each" -> $arr(nowMinutes, crowd))),
        upsert = true
      )
    _ <- elements.nonEmpty.so(update.many(elements).void)
  yield ()
