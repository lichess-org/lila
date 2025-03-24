package lila.relay

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

// We sum up the crowds of all rounds of all tours of a group
private final class RelayGroupCrowd(
    colls: RelayColls,
    groupRepo: RelayGroupRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  export cache.get

  private val cache = cacheApi[RelayTourId, Crowd](64, "relay.groupCrowd"):
    _.expireAfterWrite(14.seconds).buildAsyncFuture(compute)

  private def compute(tourId: RelayTourId): Fu[Crowd] = Crowd.from:
    for
      tourIds <- groupRepo.allTourIdsOfGroup(tourId)
      res <- colls.round
        .aggregateOne(_.sec): framework =>
          import framework.*
          Match($doc("tourId".$in(tourIds), "crowdAt".$gt(nowInstant.minus(1.hours)))) ->
            List(Group(BSONNull)("sum" -> SumField("crowd")))
    yield res.headOption.flatMap(_.int("sum")).orZero
