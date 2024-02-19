package lila.relay

import akka.stream.scaladsl.*
import play.api.libs.json.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.common.config.MaxPerSecond
import lila.db.dsl.{ *, given }
import lila.common.config.Max
import lila.relay.RelayRound.WithTour

final class RelayTourStream(
    roundRepo: RelayRoundRepo,
    tourRepo: RelayTourRepo,
    jsonView: JsonView,
    leaderboard: RelayLeaderboardApi
)(using Executor, akka.stream.Materializer):

  import BSONHandlers.given
  import JsonView.given

  def officialTourStream(perSecond: MaxPerSecond, nb: Max, withLeaderboards: Boolean): Source[JsObject, ?] =

    val lookup = $lookup.pipeline(
      from = roundRepo.coll,
      as = "rounds",
      local = "_id",
      foreign = "tourId",
      pipe = List($doc("$sort" -> roundRepo.sort.start))
    )
    val activeStream = tourRepo.coll
    // val groupLookup = $lookup.pipeline(
    //   from = colls.group,
    //   as = "rounds",
    //   local = "_id",
    //   foreign = "tourId",
    //   pipe = List($doc("$sort" -> roundRepo.sort.start))
    // )

      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(tourRepo.selectors.officialActive),
          Sort(Descending("tier")),
          PipelineOperator(lookup)
        )
      .documentSource(nb.value)

    val inactiveStream = tourRepo.coll
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(tourRepo.selectors.officialInactive),
          Sort(Descending("syncedAt")),
          PipelineOperator(lookup)
        )
      .documentSource(nb.value)

    activeStream
      .concat(inactiveStream)
      .mapConcat: doc =>
        doc
          .asOpt[RelayTour]
          .flatMap: tour =>
            doc.getAsOpt[List[RelayRound]]("rounds") map tour.withRounds
          .toList
      .throttle(perSecond.value, 1 second)
      .take(nb.value)
      .mapAsync(1): t =>
        withLeaderboards.so(leaderboard(t.tour)).map(t -> _)
      .map: (t, l) =>
        jsonView(t, withUrls = true, leaderboard = l)
  end officialTourStream
