package lila.ublog

import java.time.temporal.ChronoUnit
import java.time.{ Year, YearMonth, ZoneOffset, LocalTime }

import scala.util.Try

import reactivemongo.api.bson.BSONNull

import scalalib.paginator.{ AdapterLike, Paginator }

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi

object UblogBestOf:

  private val ublogOrigin      = YearMonth.of(2021, 9)
  private def nbMonthsBackward = 
    ublogOrigin.until(currentYearMonth, ChronoUnit.MONTHS).toInt
  
  private def currentYearMonth = YearMonth.now(ZoneOffset.UTC)
  def allYears                 = (ublogOrigin.getYear to currentYearMonth.getYear + 1).toList

  def selector(month: YearMonth) =
    val (start, end) = boundsOfMonth(month)
    // to hit topic prod index
    $doc("topics".$ne(UblogTopic.offTopic), "lived.at".$gt(start).$lt(end))

  private def boundsOfMonth(month: YearMonth): (Instant, Instant) =
    val start = month.atDay(1).atStartOfDay()
    val end   = month.atEndOfMonth().atTime(LocalTime.MAX)
    (start.toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC))

  def readYear(year: Int): Option[Year] =
    (ublogOrigin.getYear <= year && year <= currentYearMonth.getYear).so(Try(Year.of(year)).toOption)

  def readYearMonth(year: Int, month: Int): Option[YearMonth] =
    Try(YearMonth.of(year, month)).toOption.filter: ym =>
      // writing it as negative allow bounds to be included
      !(ym.isBefore(ublogOrigin) || ym.isAfter(currentYearMonth))

  private def monthsBack(n: Int): YearMonth =
    currentYearMonth.minusMonths(n)

  // from `now` go back to `offset` months and from that point gives all `length` precedecing months
  def slice(offset: Int, length: Int): Seq[(YearMonth, Int)] =
    val from = currentYearMonth.minusMonths(offset)
    (0 to length).map(x => from.minusMonths(x.toInt)).zip((0 to length))

  case class WithPosts(yearMonth: YearMonth, posts: List[UblogPost.PreviewPost])

final class UblogBestOf(colls: UblogColls, ublogApi: UblogApi, cacheApi: CacheApi)(using Executor):

  import UblogBsonHandlers.{ *, given }

  // FIXME 12 queries for one cache entry!!
  // compose aggregation to fix
  private val withPostsCache =
    // the seq is sorted by most recent first
    cacheApi[Year, Seq[UblogBestOf.WithPosts]](16, "ublog.bestOf.withPosts"):
      _.refreshAfterWrite(1.hour).buildAsyncFuture: year =>
        (1 to 12)
          .map(x => year.atMonth(x))
          .map: month =>
            ublogApi
              .aggregateVisiblePosts(UblogBestOf.selector(month), 0, 4)
              .map: preview =>
                UblogBestOf.WithPosts(month, preview)
          .parallel
          .map(_.sortBy(_.yearMonth)(Ordering[YearMonth].reverse))

  def ofYear(year: Year): Fu[Seq[UblogBestOf.WithPosts]] = withPostsCache.get(year)
  // latest 12 months rolling
  def latest =
    val curYear = Year.now(ZoneOffset.UTC)
    for
      a <- withPostsCache.get(curYear)
      b <- withPostsCache.get(curYear.minusYears(1))
    yield (a ++ b).take(12)

  def paginatorQuery(offset: Int, length: Int): Fu[List[UblogBestOf.WithPosts]] =
    colls.post
      .aggregateList(length, _.sec): framework =>
        import framework.*
        Facet(
          UblogBestOf
            .slice(offset = offset, length = length)
            .map: (month, i) =>
              s"$i" -> List(
                Match($doc("live" -> true) ++ UblogBestOf.selector(month)),
                Project(
                  previewPostProjection ++ $doc(
                    "timelessRank" -> $doc("$subtract" -> $arr("$rank", "$lived.at"))
                  )
                ),
                Sort(Descending("timelessRank")),
                Limit(4)
              )
        ) -> List(
          Project($doc("all" -> $doc("$objectToArray" -> "$$ROOT"))),
          UnwindField("all"),
          ReplaceRootField("all"),
          Project(
            $doc(
              "v"          -> true,
              "monthsBack" -> $doc("$toInt" -> "$k")
            )
          ),
          Sort(Ascending("monthsBack"))
        )
      .map: docs =>
        for
          doc        <- docs
          monthsBack <- doc.int("monthsBack")
          yearMonth = UblogBestOf.monthsBack(offset + monthsBack)
          posts <- doc.getAsOpt[List[UblogPost.PreviewPost]]("v")
        yield UblogBestOf.WithPosts(yearMonth, posts)

  private val maxPerPage = MaxPerPage(12) // a year

  def liveByYear(page: Int): Fu[Paginator[UblogBestOf.WithPosts]] =
    Paginator(
      adapter = new AdapterLike[UblogBestOf.WithPosts]:
        def nbResults: Fu[Int] = fuccess(UblogBestOf.nbMonthsBackward)
        def slice(offset: Int, length: Int) =
          paginatorQuery(offset = offset, length = length)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )

