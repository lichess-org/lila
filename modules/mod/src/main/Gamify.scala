package lila.mod

import lila.db.BSON.BSONJodaDateTimeHandler
import org.joda.time.DateTime
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.report.Room
import lila.user.User.lichessId

final class Gamify(
    logColl: Coll,
    reportApi: lila.report.ReportApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    historyColl: Coll
) {

  import Gamify._
  import lila.report.BSONHandlers.RoomBSONHandler

  def history(orCompute: Boolean = true): Fu[List[HistoryMonth]] = {
    val until = DateTime.now minusMonths 1 withDayOfMonth 1
    val lastId = HistoryMonth.makeId(until.getYear, until.getMonthOfYear)
    historyColl.find($empty).sort($doc(
      "year" -> -1,
      "month" -> -1
    )).cursor[HistoryMonth]().gather[List]().flatMap { months =>
      months.headOption match {
        case Some(m) if m._id == lastId => fuccess(months)
        case _ if !orCompute => fuccess(months)
        case Some(m) => buildHistoryAfter(m.year, m.month, until) >> history(false)
        case _ => buildHistoryAfter(2017, 6, until) >> history(false)
      }
    }
  }

  private implicit val modMixedBSONHandler = Macros.handler[ModMixed]
  private implicit val historyMonthBSONHandler = Macros.handler[HistoryMonth]

  private def buildHistoryAfter(afterYear: Int, afterMonth: Int, until: DateTime): Funit =
    (afterYear to until.getYear).flatMap { year =>
      ((year == afterYear).fold(afterMonth + 1, 1) to
        (year == until.getYear).fold(until.getMonthOfYear, 12)).map { month =>
          mixedLeaderboard(
            after = new DateTime(year, month, 1, 0, 0).pp("compute mod history"),
            before = new DateTime(year, month, 1, 0, 0).plusMonths(1).some
          ).map {
              _.headOption.map { champ =>
                HistoryMonth(HistoryMonth.makeId(year, month), year, month, champ)
              }
            }
        }.toList
    }.toList.sequenceFu.map(_.flatten).flatMap {
      _.map { month =>
        historyColl.update($doc("_id" -> month._id), month, upsert = true).void
      }.sequenceFu
    }.void

  def leaderboards = leaderboardsCache.get

  private val leaderboardsCache = asyncCache.single[Leaderboards](
    name = "mod.leaderboards",
    f = mixedLeaderboard(DateTime.now minusDays 1, none) zip
      mixedLeaderboard(DateTime.now minusWeeks 1, none) zip
      mixedLeaderboard(DateTime.now minusMonths 1, none) map {
        case ((daily, weekly), monthly) => Leaderboards(daily, weekly, monthly)
      },
    expireAfter = _.ExpireAfterWrite(60 seconds)
  )

  private def mixedLeaderboard(after: DateTime, before: Option[DateTime]): Fu[List[ModMixed]] =
    actionLeaderboard(after, before) zip reportLeaderboard(after, before) map {
      case (actions, reports) => actions.map(_.modId) intersect reports.map(_.modId) map { modId =>
        ModMixed(
          modId,
          action = actions.find(_.modId == modId) ?? (_.count),
          report = reports.find(_.modId == modId) ?? (_.count)
        )
      } sortBy (-_.score)
    }

  private def dateRange(from: DateTime, toOption: Option[DateTime]) =
    $doc("$gte" -> from) ++ toOption.?? { to => $doc("$lt" -> to) }

  private val notLichess = $doc("$ne" -> lichessId)

  private def actionLeaderboard(after: DateTime, before: Option[DateTime]): Fu[List[ModCount]] =
    logColl.aggregateList(Match($doc(
      "date" -> dateRange(after, before),
      "mod" -> notLichess
    )), List(
      GroupField("mod")("nb" -> SumValue(1)),
      Sort(Descending("nb"))
    ), maxDocs = 100).map {
      _.flatMap { obj =>
        obj.getAs[String]("_id") |@| obj.getAs[Int]("nb") apply ModCount.apply
      }
    }

  private def reportLeaderboard(after: DateTime, before: Option[DateTime]): Fu[List[ModCount]] =
    reportApi.coll.aggregateList(
      Match($doc(
        "atoms.0.at" -> dateRange(after, before),
        "room" $in Room.all, // required to make use of the mongodb index room+atoms.0.at
        "processedBy" -> notLichess
      )), List(
        GroupField("processedBy")("nb" -> SumValue(1)),
        Sort(Descending("nb"))
      ),
      maxDocs = Int.MaxValue,
      readPreference = ReadPreference.secondaryPreferred
    ).map {
        _.flatMap { obj =>
          obj.getAs[String]("_id") |@| obj.getAs[Int]("nb") apply ModCount.apply
        }
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
    case object Day extends Period
    case object Week extends Period
    case object Month extends Period
    def apply(p: String) = List(Day, Week, Month).find(_.name == p)
  }

  case class Leaderboards(daily: List[ModMixed], weekly: List[ModMixed], monthly: List[ModMixed]) {
    def apply(period: Period) = period match {
      case Period.Day => daily
      case Period.Week => weekly
      case Period.Month => monthly
    }
  }

  case class ModCount(modId: String, count: Int)
  case class ModMixed(modId: String, action: Int, report: Int) {
    def score = action + report
  }
}
