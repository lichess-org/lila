package lila.relay

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

// We sum up the crowds of all rounds of all tours of a group
private final class RelayGroupCrowd(colls: RelayColls, cacheApi: lila.memo.CacheApi)(using Executor):

  export cache.get

  private val cache = cacheApi[RelayTourId, Crowd](64, "relay.groupCrowd"):
    _.expireAfterWrite(14.seconds).buildAsyncFuture(compute)

  private def compute(tourId: RelayTourId): Fu[Crowd] = Crowd.from:
    colls.group
      .aggregateOne(_.sec): framework =>
        import framework.*
        Match($doc("tours" -> tourId)) -> List(
          PipelineOperator:
            $lookup.pipelineFull(
              from = colls.round.name,
              as = "round",
              let = $doc("tid" -> "$tours"),
              pipe = List(
                $doc(
                  "$match" -> $doc(
                    "$expr"   -> $doc("$in" -> $arr("$tourId", "$$tid")),
                    "crowdAt" -> $doc("$gt" -> nowInstant.minus(1.hours))
                  )
                ),
                $doc("$project" -> $doc("_id" -> false, "crowd" -> true))
              )
            )
          ,
          Project($doc("_id" -> false, "round" -> true)),
          UnwindField("round"),
          Group(BSONNull)("sum" -> SumField("round.crowd"))
        )
      .map(_.headOption.flatMap(_.int("sum")).orZero)
