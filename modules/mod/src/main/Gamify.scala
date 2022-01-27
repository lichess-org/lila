package lila.mod

import lila.db.BSON.BSONJodaDateTimeHandler
import org.joda.time.DateTime
import reactivemongo.api._
import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.CacheApi._
import lila.report.Room
import lila.user.User

final class Gamify(
    logRepo: ModlogRepo,
    reportApi: lila.report.ReportApi,
    modApi: lila.mod.ModApi,
    cacheApi: lila.memo.CacheApi,
    historyRepo: HistoryRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Gamify._
  import lila.report.BSONHandlers.RoomBSONHandler

  implicit private val modMixedBSONHandler     = Macros.handler[ModMixed]
  implicit private val historyMonthBSONHandler = Macros.handler[HistoryMonth]

  def history(orCompute: Boolean = true): Fu[List[HistoryMonth]] = {
    val until  = DateTime.now minusMonths 1 withDayOfMonth 1
    val lastId = HistoryMonth.makeId(until.getYear, until.getMonthOfYear)
    historyRepo.coll
      .find($empty)
      .sort(
        $doc(
          "year"  -> -1,
          "month" -> -1
        )
      )
      .cursor[HistoryMonth]()
      .list() flatMap { months =>
      months.headOption match {
        case Some(m) if m._id == lastId => fuccess(months)
        case _ if !orCompute            => fuccess(months)
        case Some(m)                    => buildHistoryAfter(m.year, m.month, until) >> history(false)
        case _                          => buildHistoryAfter(2017, 6, until) >> history(false)
      }
    }
  }

  private def buildHistoryAfter(afterYear: Int, afterMonth: Int, until: DateTime): Funit =
    (afterYear to until.getYear)
      .flatMap { year =>
        ((if (year == afterYear) afterMonth + 1 else 1) to
          (if (year == until.getYear) until.getMonthOfYear else 12)).map { month =>
          mixedLeaderboard(
            after = new DateTime(year, month, 1, 0, 0).pp("compute mod history"),
            before = new DateTime(year, month, 1, 0, 0).plusMonths(1).some
          ).map {
            _.headOption.map { champ =>
              HistoryMonth(HistoryMonth.makeId(year, month), year, month, champ)
            }
          }
        }.toList
      }
      .toList
      .sequenceFu
      .map(_.flatten)
      .flatMap {
        _.map { month =>
          historyRepo.coll.update.one($doc("_id" -> month._id), month, upsert = true).void
        }.sequenceFu
      }
      .void

  def leaderboards = leaderboardsCache.getUnit

  private val leaderboardsCache = cacheApi.unit[Leaderboards] {
    _.expireAfterWrite(10 minutes)
      .buildAsyncFuture { _ =>
        mixedLeaderboard(DateTime.now minusDays 1, none) zip
          mixedLeaderboard(DateTime.now minusWeeks 1, none) zip
          mixedLeaderboard(DateTime.now minusMonths 1, none) map { case ((daily, weekly), monthly) =>
            Leaderboards(daily, weekly, monthly)
          }
      }
  }

  private def mixedLeaderboard(after: DateTime, before: Option[DateTime]): Fu[List[ModMixed]] =
    for {
      actions <- actionLeaderboard(after, before)
      reports <- reportLeaderboard(after, before)
      modList <- modApi.allMods
    } yield actions.map(_.modId) intersect modList.map(_.id) diff hidden map { modId =>
      ModMixed(
        modId,
        action = actions.find(_.modId == modId) ?? (_.count),
        report = reports.find(_.modId == modId) ?? (_.count)
      )
    } sortBy (-_.score)

  private def dateRange(from: DateTime, toOption: Option[DateTime]) =
    $doc("$gte" -> from) ++ toOption.?? { to =>
      $doc("$lt" -> to)
    }

  private val hidden = List(User.lichessId, "irwin")

  private def actionLeaderboard(after: DateTime, before: Option[DateTime]): Fu[List[ModCount]] =
    logRepo.coll
      .aggregateList(maxDocs = 100, readPreference = ReadPreference.secondaryPreferred) { framework =>
        import framework._
        Match(
          $doc(
            "date" -> dateRange(after, before),
            "mod"  -> $nin(hidden)
          )
        ) -> List(
          GroupField("mod")("nb" -> SumAll),
          Sort(Descending("nb"))
        )
      }
      .map {
        _.flatMap { obj =>
          import cats.implicits._
          (obj.string("_id"), obj.int("nb")) mapN ModCount.apply
        }
      }

  private def reportLeaderboard(after: DateTime, before: Option[DateTime]): Fu[List[ModCount]] =
    reportApi.coll
      .aggregateList(
        maxDocs = Int.MaxValue,
        readPreference = ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
        Match(
          $doc(
            "done.at" -> dateRange(after, before),
            "done.by" -> $nin(hidden),
            "open"    -> false
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
      }
      .map { docs =>
        for {
          doc <- docs
          id  <- doc.string("_id")
          nb  <- doc.int("nb")
        } yield ModCount(id, nb)
      }
}

object Gamify {

  case class HistoryMonth(_id: String, year: Int, month: Int, champion: ModMixed) {
    def date = new DateTime(year, month, 1, 0, 0)
  }
  object HistoryMonth {
    def makeId(year: Int, month: Int) = s"$year/$month"
  }

  sealed trait Period {
    def name = toString.toLowerCase
  }
  object Period {
    case object Day   extends Period
    case object Week  extends Period
    case object Month extends Period
    def apply(p: String) = List(Day, Week, Month).find(_.name == p)
  }

  case class Leaderboards(daily: List[ModMixed], weekly: List[ModMixed], monthly: List[ModMixed]) {
    def apply(period: Period) =
      period match {
        case Period.Day   => daily
        case Period.Week  => weekly
        case Period.Month => monthly
      }
  }

  case class ModCount(modId: User.ID, count: Int)
  case class ModMixed(modId: User.ID, action: Int, report: Int) {
    def score = action + report
  }
}
