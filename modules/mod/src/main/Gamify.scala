package lila.mod

import lila.db.BSON.BSONJodaDateTimeHandler
import org.joda.time.DateTime
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.Implicits._
import lila.memo.AsyncCache

final class Gamify(
    logColl: Coll,
    reportColl: Coll) {

  import Gamify._

  def leaderboards = leaderboardsCache(true)

  private val leaderboardsCache = AsyncCache.single[Leaderboards](
    f = mixedLeaderboard(_ minusDays 1) zip
      mixedLeaderboard(_ minusWeeks 1) zip
      mixedLeaderboard(_ minusMonths 1) map {
        case ((daily, weekly), monthly) => Leaderboards(daily, weekly, monthly)
      },
    timeToLive = 10 seconds)

  private def mixedLeaderboard(since: DateTime => DateTime): Fu[List[ModMixed]] =
    actionLeaderboard(since) zip reportLeaderboard(since) map {
      case (actions, reports) => actions.map(_.modId) intersect reports.map(_.modId) map { modId =>
        ModMixed(modId,
          action = actions.find(_.modId == modId) ?? (_.count),
          report = reports.find(_.modId == modId) ?? (_.count))
      } sortBy (-_.score)
    }

  private def after(date: DateTime) = BSONDocument("$gte" -> date)
  private val notLichess = BSONDocument("$ne" -> "lichess")

  private def actionLeaderboard(since: DateTime => DateTime): Fu[List[ModCount]] =
    logColl.aggregate(Match(BSONDocument(
      "date" -> after(since(DateTime.now)),
      "mod" -> notLichess
    )), List(
      GroupField("mod")("nb" -> SumValue(1)),
      Sort(Descending("nb"))
    )).map {
      _.documents.flatMap { obj =>
        obj.getAs[String]("_id") |@| obj.getAs[Int]("nb") apply ModCount.apply
      }
    }

  private def reportLeaderboard(since: DateTime => DateTime): Fu[List[ModCount]] =
    reportColl.aggregate(
      Match(BSONDocument(
        "createdAt" -> after(since(DateTime.now)),
        "processedBy" -> notLichess
      )), List(
        GroupField("processedBy")("nb" -> SumValue(1)),
        Sort(Descending("nb"))
      )).map {
        _.documents.flatMap { obj =>
          obj.getAs[String]("_id") |@| obj.getAs[Int]("nb") apply ModCount.apply
        }
      }
}

object Gamify {

  sealed trait Period
  object Period {
    case object Day extends Period
    case object Week extends Period
    case object Month extends Period
    def apply(p: String) = List(Day, Week, Month).find(_.toString.toLowerCase == p)
  }

  case class Leaderboards(daily: List[ModMixed], weekly: List[ModMixed], monthly: List[ModMixed]) {
    def apply(period: Period) = period match {
      case Period.Day   => daily
      case Period.Week  => weekly
      case Period.Month => monthly
    }
  }

  case class ModCount(modId: String, count: Int)
  case class ModMixed(modId: String, action: Int, report: Int) {
    def score = action + report
  }
}
