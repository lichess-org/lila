package lila.relay

import akka.stream.scaladsl.*
import play.api.libs.json.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.db.dsl.*

final class RelayTourStream(colls: RelayColls, jsonView: JsonView)(using akka.stream.Materializer):

  import BSONHandlers.given
  import RelayTourRepo.selectors

  private val roundLookup = $lookup.simple(
    from = colls.round,
    as = "rounds",
    local = "_id",
    foreign = "tourId",
    pipe = List($doc("$sort" -> RelayRoundRepo.sort.asc))
  )

  def officialTourStream(perSecond: MaxPerSecond, nb: Max)(using JsonView.Config): Source[JsObject, ?] =
    val activeStream = colls.tour
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(selectors.officialActive),
          Sort(Descending("tier")),
          PipelineOperator(roundLookup)
        )
      .documentSource(nb.value)

    val inactiveStream = colls.tour
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(selectors.officialInactive),
          Sort(Descending("syncedAt")),
          PipelineOperator(roundLookup)
        )
      .documentSource(nb.value)

    activeStream
      .concat(inactiveStream)
      .mapConcat: doc =>
        doc
          .asOpt[RelayTour]
          .flatMap: tour =>
            doc.getAsOpt[List[RelayRound]]("rounds").map(tour.withRounds)
          .toList
      .throttle(perSecond.value, 1.second)
      .take(nb.value)
      .map(jsonView.fullTourWithRounds(_, group = none))
