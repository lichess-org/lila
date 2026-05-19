package lila.relay

import akka.stream.scaladsl.*
import play.api.libs.json.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.db.dsl.{ given, * }

final class RelayTourStream(colls: RelayColls, jsonView: RelayJsonView)(using akka.stream.Materializer):

  import BSONHandlers.given
  import RelayTourRepo.selectors

  private val roundLookup = $lookup.simple(
    from = colls.round,
    as = "rounds",
    local = "_id",
    foreign = "tourId",
    pipe = List($doc("$sort" -> RelayRoundRepo.sort.asc))
  )

  def officialTourStream(perSecond: MaxPerSecond, nb: Max, liveOnly: Boolean)(using
      RelayJsonView.Config,
      lila.core.i18n.Translate
  ): Source[JsObject, ?] =

    def activeStream = colls.tour
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(selectors.officialActive ++ liveOnly.so(selectors.live)),
          Sort(Descending("tier")),
          PipelineOperator(roundLookup)
        )
      .documentSource(nb.value)

    def inactiveStream = colls.tour
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(selectors.officialInactive),
          Sort(Descending("syncedAt")),
          PipelineOperator(roundLookup)
        )
      .documentSource(nb.value)

    val fullStream = if liveOnly then activeStream
    else activeStream.concat(inactiveStream)

    fullStream
      .mapConcat: doc =>
        doc
          .asOpt[RelayTour]
          .flatMap: tour =>
            doc.getAsOpt[List[RelayRound]]("rounds").map(tour.withRounds)
          .toList
      .throttle(perSecond.value, 1.second)
      .take(nb.value)
      .map(jsonView.fullTourWithRounds(_, group = none))
