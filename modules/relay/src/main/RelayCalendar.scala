package lila.relay

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.relay.RelayTour.WithLastRound
import lila.relay.RelayTourRepo.*
import java.time.YearMonth

final class RelayCalendar(
    tourRepo: RelayTourRepo,
    roundRepo: RelayRoundRepo,
    colls: RelayColls,
    cacheApi: CacheApi
)(using Executor):

  def atMonth(at: YearMonth): Fu[List[WithLastRound]] =
    val max = 100
    tourRepo.coll
      .aggregateList(max, _.sec): framework =>
        import framework.*
        Match(selectors.officialPublic ++ selectors.inMonth(at)) -> {
          List(Sort(Descending("syncedAt"))) :::
            tourRepo.aggregateRoundAndUnwind(colls, framework) :::
            List(Limit(max))
        }
      .map(readToursWithRound)

  def readMonth(year: Int, month: Int): Option[YearMonth] =
    util.Try(YearMonth.of(year, month)).toOption

object RelayCalendar:

  lazy val allYears: List[Int] = (2020 to java.time.LocalDate.now.getYear).toList
