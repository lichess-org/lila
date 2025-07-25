package lila.mod

import reactivemongo.api.*
import reactivemongo.api.bson.*

import java.time.{ Instant, LocalDateTime }

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*
import lila.report.Room

final class Gamify(
    logRepo: ModlogRepo,
    reportApi: lila.report.ReportApi,
    modApi: lila.mod.ModApi,
    cacheApi: lila.memo.CacheApi,
    historyRepo: HistoryRepo
)(using Executor):

  import Gamify.*

  private given BSONDocumentHandler[ModMixed] = Macros.handler
  private given BSONDocumentHandler[HistoryMonth] = Macros.handler

  def history(orCompute: Boolean = true): Fu[List[HistoryMonth]] =
    val until = nowDateTime.minusMonths(1).withDayOfMonth(1)
    val lastId = HistoryMonth.makeId(until.getYear, until.getMonthValue)
    historyRepo.coll
      .find($empty)
      .sort(
        $doc(
          "year" -> -1,
          "month" -> -1
        )
      )
      .cursor[HistoryMonth]()
      .listAll()
      .flatMap { months =>
        months.headOption match
          case Some(m) if m._id == lastId => fuccess(months)
          case _ if !orCompute => fuccess(months)
          case Some(m) => buildHistoryAfter(m.year, m.month, until) >> history(false)
          case _ => buildHistoryAfter(2017, 6, until) >> history(false)
      }

  private def buildHistoryAfter(afterYear: Int, afterMonth: Int, until: LocalDateTime): Funit =
    (afterYear to until.getYear)
      .flatMap { year =>
        ((if year == afterYear then afterMonth + 1 else 1) to
          (if year == until.getYear then until.getMonthValue else 12)).map { month =>
          mixedLeaderboard(
            after = instantOf(year, month, 1, 0, 0),
            before = instantOf(year, month, 1, 0, 0).plusMonths(1).some
          ).map:
            _.headOption.map { champ =>
              HistoryMonth(HistoryMonth.makeId(year, month), year, month, champ)
            }
        }.toList
      }
      .toList
      .parallel
      .map(_.flatten)
      .flatMap:
        _.parallelVoid: month =>
          historyRepo.coll.update.one($doc("_id" -> month._id), month, upsert = true).void

  def leaderboards = leaderboardsCache.getUnit

  private val leaderboardsCache = cacheApi.unit[Leaderboards]:
    _.expireAfterWrite(10.minutes)
      .buildAsyncFuture { _ =>
        mixedLeaderboard(nowInstant.minusDays(1), none)
          .zip(mixedLeaderboard(nowInstant.minusWeeks(1), none))
          .zip(mixedLeaderboard(nowInstant.minusMonths(1), none))
          .map { case ((daily, weekly), monthly) =>
            Leaderboards(daily, weekly, monthly)
          }
      }

  private def mixedLeaderboard(after: Instant, before: Option[Instant]): Fu[List[ModMixed]] =
    for
      actions <- actionLeaderboard(after, before)
      reports <- reportLeaderboard(after, before)
      modList <- modApi.allMods
    yield actions
      .map(_.modId)
      .intersect(modList.map(_.id))
      .diff(hidden)
      .map { modId =>
        ModMixed(
          modId,
          action = actions.find(_.modId == modId).so(_.count),
          report = reports.find(_.modId == modId).so(_.count)
        )
      }
      .sortBy(-_.score)

  private def dateRange(from: Instant, toOption: Option[Instant]) =
    $doc("$gte" -> from) ++ toOption.so { to =>
      $doc("$lt" -> to)
    }

  private val hidden = List(UserId.lichess, UserId.irwin)

  private def actionLeaderboard(after: Instant, before: Option[Instant]): Fu[List[ModCount]] =
    logRepo.coll
      .aggregateList(maxDocs = 100, _.sec): framework =>
        import framework.*
        Match(
          $doc(
            "date" -> dateRange(after, before),
            "mod" -> $nin(hidden)
          )
        ) -> List(
          GroupField("mod")("nb" -> SumAll),
          Sort(Descending("nb")),
          Limit(100)
        )
      .map:
        _.flatMap: obj =>
          (obj.getAsOpt[UserId]("_id"), obj.int("nb")).mapN(ModCount.apply)

  private def reportLeaderboard(after: Instant, before: Option[Instant]): Fu[List[ModCount]] =
    reportApi.coll
      .aggregateList(maxDocs = Int.MaxValue, _.sec): framework =>
        import framework.*
        Match(
          $doc(
            "done.at" -> dateRange(after, before),
            "done.by" -> $nin(hidden),
            "open" -> false
          )
        ) -> List(
          GroupField("done.by")(
            "nb" -> Sum(
              $doc(
                "$cond" -> $arr($doc("$eq" -> $arr("$room", Room.Cheat.key)), 3, 1)
              )
            )
          ),
          Sort(Descending("nb"))
        )
      .map: docs =>
        for
          doc <- docs
          id <- doc.getAsOpt[UserId]("_id")
          nb <- doc.int("nb")
        yield ModCount(id, nb)

object Gamify:

  case class HistoryMonth(_id: String, year: Int, month: Int, champion: ModMixed):
    lazy val date = LocalDateTime.of(year, month, 1, 0, 0)
  object HistoryMonth:
    def makeId(year: Int, month: Int) = s"$year/$month"

  enum Period:
    def name = Period.this.toString.toLowerCase
    case Day, Week, Month
  object Period:
    def apply(p: String) = values.find(_.name == p)

  case class Leaderboards(daily: List[ModMixed], weekly: List[ModMixed], monthly: List[ModMixed]):
    def apply(period: Period) =
      period match
        case Period.Day => daily
        case Period.Week => weekly
        case Period.Month => monthly

  case class ModCount(modId: UserId, count: Int)
  case class ModMixed(modId: UserId, action: Int, report: Int):
    def score = action + report
