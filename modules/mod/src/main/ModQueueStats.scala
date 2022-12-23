package lila.mod

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.*
import reactivemongo.api.ReadPreference
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.util.Try

import lila.db.dsl.{ *, given }
import lila.mod.ModActivity.{ dateFormat, Period }
import lila.report.Room

final class ModQueueStats(
    cacheApi: lila.memo.CacheApi,
    repo: ModQueueStatsRepo
)(using ec: ExecutionContext):

  import ModQueueStats.*

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
      .cursor[Bdoc](temporarilyPrimary)
      .listAll()
      .map { docs =>
        for {
          doc     <- docs
          dateStr <- doc.string("_id")
          date    <- Try(dateFormat parseDateTime dateStr).toOption
          data    <- doc.getAsOpt[List[Bdoc]]("data")
        } yield date -> {
          for {
            entry <- data
            nb    <- entry.int("nb")
            room  <- entry.string("room")
            score <- entry.int("score")
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
            "rooms" -> Room.values
              .map { room => room.key -> room.name }
              .appendedAll {
                List(
                  "appeal"   -> "Appeal",
                  "streamer" -> "Streamer"
                )
              }
              .map { case (roomKey, roomName) =>
                Json.obj(
                  "name" -> roomName,
                  "series" -> scores.collect {
                    case score if score > 20 || roomKey == Room.Boost.key =>
                      Json.obj(
                        "name" -> score,
                        "data" -> days.map(~_._2.collectFirst {
                          case (r, s, nb) if r == roomKey && s == score => nb
                        })
                      )
                  }
                )
              }
          )
        )
      }

object ModQueueStats:

  type Score = Int
  type Nb    = Int

  val scores = List[Score](20, 40, 60, 80)

  case class Result(period: Period, json: JsObject)
