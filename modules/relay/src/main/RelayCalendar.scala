package lila.relay

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.relay.RelayTour.WithFirstRound
import lila.relay.RelayTourRepo.*
import java.time.{ LocalDate, YearMonth }

final class RelayCalendar(
    tourRepo: RelayTourRepo,
    roundRepo: RelayRoundRepo,
    colls: RelayColls,
    cacheApi: CacheApi
)(using Executor):

  def atMonth(at: YearMonth): Fu[List[WithFirstRound]] =
    val max = 200
    tourRepo.coll
      .aggregateList(max, _.sec): framework =>
        import framework.*
        // select the first round of the tour, that starts in that month
        val roundPipeline = List(
          $doc("$sort" -> RelayRoundRepo.sort.asc),
          $doc(
            "$addFields" -> $doc(
              "sync.log"  -> $arr(),
              "startDate" -> $doc("$ifNull" -> $arr("$startedAt", "$startsAt"))
            )
          ),
          $doc(
            "$match" -> $doc(
              "startDate" -> $doc("$gte" -> LocalDate.of(at.getYear, at.getMonth, 1))
            )
          ),
          $doc("$limit" -> 1)
        )
        Match(selectors.officialPublic ++ selectors.inMonth(at)) -> {
          tourRepo.aggregateRoundAndUnwind(colls, framework, roundPipeline = roundPipeline.some) :::
            List(Sort(Ascending("round.startDate"))) :::
            List(Limit(max))
        }
      .map(readToursWithRound(RelayTour.WithFirstRound.apply))

  def readMonth(year: Int, month: Int): Option[YearMonth] =
    util.Try(YearMonth.of(year, month)).toOption

object RelayCalendar:

  export YearMonth.now

  lazy val allYears: List[Int] = (2020 to java.time.LocalDate.now.getYear + 1).toList
