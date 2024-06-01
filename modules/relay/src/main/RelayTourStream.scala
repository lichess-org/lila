package lila.relay

import akka.stream.scaladsl.*
import play.api.libs.json.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final class RelayTourStream(
    colls: RelayColls,
    jsonView: JsonView,
    leaderboard: RelayLeaderboardApi
)(using Executor, akka.stream.Materializer):

  import BSONHandlers.given
  import RelayTourRepo.selectors

  private val roundLookup = $lookup.pipeline(
    from = colls.round,
    as = "rounds",
    local = "_id",
    foreign = "tourId",
    pipe = List($doc("$sort" -> RelayRoundRepo.sort.start))
  )

  def officialTourStream(perSecond: MaxPerSecond, nb: Max): Source[JsObject, ?] =
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
      .throttle(perSecond.value, 1 second)
      .take(nb.value)
      .map(jsonView(_))

  def userTourStream(perSecond: MaxPerSecond, owner: UserId, nb: Max, page: Int)(using
      me: Option[MyId]
  ): Source[JsObject, ?] =
    val isMe = me.exists(_.is(owner))

    val broadcasts = colls.tour
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(selectors.ownerId(owner.id) ++ (!isMe).so(selectors.publicTour)),
          Sort(Descending("createdAt")),
          PipelineOperator(roundLookup)
        )
      .documentSource(nb.value)

    broadcasts
      .mapConcat: doc =>
        doc
          .asOpt[RelayTour]
          .flatMap: tour =>
            doc.getAsOpt[List[RelayRound]]("rounds").map(tour.withRounds)
          .toList
      .throttle(perSecond.value, 1 second)
      .take(nb.value)
      .map(jsonView(_))
