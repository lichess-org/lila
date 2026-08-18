package lila.relay

import org.apache.pekko.stream.scaladsl.*
import play.api.libs.json.*
import reactivemongo.pekkostream.cursorProducer
import reactivemongo.api.bson.*

import lila.db.dsl.{ given, * }
import lila.common.Json.given

final class RelayTourStream(colls: RelayColls, jsonView: RelayJsonView)(using
    org.apache.pekko.stream.Materializer
):

  import RelayTourRepo.selectors

  private val roundLookup = $lookup.simple(
    from = colls.round,
    as = "rounds",
    local = "_id",
    foreign = "tourId",
    pipe = List($doc("$sort" -> RelayRoundRepo.sort.asc))
  )
  private val groupLookup = $lookup.pipelineFull(
    from = colls.group.name,
    as = "group",
    let = $doc("tourId" -> "$_id"),
    pipe = List(
      $doc("$match" -> $doc("$expr" -> $doc("$in" -> $arr("$$tourId", "$tours")))),
      $doc("$project" -> $doc("_id" -> false, "name" -> true))
    )
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
          PipelineOperator(roundLookup),
          PipelineOperator(groupLookup)
        )
      .documentSource(nb.value)

    def inactiveStream = colls.tour
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(selectors.officialInactive),
          Sort(Descending("syncedAt")),
          PipelineOperator(roundLookup),
          PipelineOperator(groupLookup)
        )
      .documentSource(nb.value)

    val fullStream = if liveOnly then activeStream
    else activeStream.concat(inactiveStream)

    fullStream
      .mapConcat(RelayTourRepo.readTourWithRoundsAndGroup)
      .throttle(perSecond.value, 1.second)
      .take(nb.value)
      .map: (t, g) =>
        jsonView.fullTourWithRounds(t, none).add("group", g)
