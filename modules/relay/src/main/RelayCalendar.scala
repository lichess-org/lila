package lila.relay

import play.api.mvc.RequestHeader

import java.time.{ LocalDate, YearMonth }

import lila.common.HTTPRequest
import lila.db.dsl.*
import lila.memo.CacheApi
import lila.relay.RelayTour.WithFirstRound
import lila.relay.RelayTourRepo.*

final class RelayCalendar(
    tourRepo: RelayTourRepo,
    colls: RelayColls,
    cacheApi: CacheApi
)(using Executor):

  val ttl = 1.hour

  private val cache = cacheApi[YearMonth, List[WithFirstRound]](64, "relay.calendar.at"):
    _.expireAfterWrite(ttl).buildAsyncFuture: at =>
      val max = 200
      val firstDay = LocalDate.of(at.getYear, at.getMonth, 1)
      tourRepo.coll
        .aggregateList(max, _.sec): framework =>
          import framework.*
          // select the first round of the tour, that starts in that month
          val roundPipeline = List(
            bdoc("$sort" -> RelayRoundRepo.sort.asc),
            bdoc(
              "$addFields" -> bdoc(
                "sync.log" -> barr(),
                "startDate" -> bdoc("$ifNull" -> barr("$startedAt", "$startsAt"))
              )
            ),
            bdoc(
              "$match" -> bdoc(
                "startDate" -> bdoc(
                  "$gte" -> firstDay,
                  "$lt" -> firstDay.plusMonths(1)
                )
              )
            ),
            bdoc("$limit" -> 1)
          )
          Match(selectors.officialPublic ++ selectors.inMonth(at)) -> {
            // reduce cache size by unselecting some fields
            Project(RelayTourRepo.unsetHeavyOptionalFields) ::
              tourRepo.aggregateRoundAndUnwind(colls, framework, roundPipeline = roundPipeline.some) :::
              List(Sort(Ascending("round.startDate"))) :::
              List(Limit(max))
          }
        .map(readToursWithRoundAndGroup(WithFirstRound.apply))

  def atMonth(at: YearMonth): Fu[List[WithFirstRound]] = cache.get(at)

  def atMonthWithReqTiersFilter(at: YearMonth)(using req: RequestHeader): Fu[List[WithFirstRound]] =
    val inTiers =
      HTTPRequest.queryStringGet("tiers").orZero.split(",").flatMap(RelayTour.Tier.byKey.get).toSet
    atMonth(at).map: tours =>
      if inTiers.isEmpty then tours
      else tours.filter(_.tour.tier.exists(inTiers))

  def readMonth(year: Int, month: Int): Option[YearMonth] =
    RelayCalendar.allYears
      .contains(year)
      .so(util.Try(YearMonth.of(year, month)).toOption)

object RelayCalendar:

  export YearMonth.now

  lazy val allYears: List[Int] = (2020 to java.time.LocalDate.now.getYear + 1).toList
