package lila.mod

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.Try

import lila.db.dsl._
import lila.mod.ModActivity.{ dateFormat, Period }
import lila.report.Room

final class ModQueueStats(
    reportApi: lila.report.ReportApi,
    cacheApi: lila.memo.CacheApi,
    repo: ModQueueStatsRepo
)(implicit
    ec: ExecutionContext
) {

  import ModQueueStats._

  def apply(period: String): Fu[Result] =
    cache.get(Period(period))

  private val cache = cacheApi[Period, Result](64, "mod.activity") {
    _.expireAfter[Period, Result](
      create = (key, _) =>
        key match {
          case Period.Week  => 15.seconds
          case Period.Month => 5.minutes
          case Period.Year  => 1.day
        },
      update = (_, _, current) => current,
      read = (_, _, current) => current
    ).buildAsyncFuture(compute)
  }

  private def compute(period: Period): Fu[Result] =
    repo.coll
      .find($doc("_id" $gte dateFormat.print(Period dateSince period)))
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .list()
      .map { docs =>
        for {
          doc     <- docs
          dateStr <- doc.string("_id")
          date    <- Try(dateFormat parseDateTime dateStr).toOption
          data    <- doc.getAsOpt[List[Bdoc]]("data")
        } yield date -> {
          for {
            entry   <- data
            nb      <- entry.int("nb")
            roomStr <- entry.string("room")
            room    <- Room.byKey get roomStr
            score   <- entry.int("score")
          } yield (room, score, nb)
        }
      }
      .map { days =>
        Result(
          period,
          Json.obj(
            "common" -> Json.obj(
              "xaxis" -> days.map(_._1.getMillis)
            ),
            "rooms" -> Room.all.map { room =>
              Json.obj(
                "name" -> room.name,
                "series" -> scores.map { score =>
                  Json.obj(
                    "name" -> score,
                    "data" -> days.map(~_._2.collectFirst {
                      case (r, s, nb) if r == room && s == score => nb
                    })
                  )
                }
              )
            }
          )
        )
      }
}

object ModQueueStats {

  type Score = Int
  type Nb    = Int

  val scores = List[Score](20, 40, 60, 80)

  case class Result(period: Period, json: JsObject)
}
