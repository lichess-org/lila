package lila.analyse

import reactivemongo.api.bson.{ BSONBoolean, BSONInteger }
import org.joda.time.format.DateTimeFormat
import lila.db.dsl.{ given, * }
import lila.memo.CacheApi
import lila.user.User

final class RequesterApi(coll: Coll)(using Executor):

  private val formatter = DateTimeFormat forPattern "yyyy-MM-dd"

  def add(requester: UserId, ownGame: Boolean): Funit =
    coll.update
      .one(
        $id(requester),
        $inc(
          "total"                  -> 1,
          formatter.print(nowDate) -> (if (ownGame) 1 else 2)
        ),
        upsert = true
      )
      .void

  def countTodayAndThisWeek(userId: UserId): Fu[(Int, Int)] =
    val now = nowDate
    coll
      .one(
        $id(userId),
        $doc {
          (7 to 0 by -1).toList.map(now.minusDays).map(formatter.print).map(_ -> BSONBoolean(true))
        }
      )
      .map { doc =>
        val daily = doc.flatMap(_ int formatter.print(now))
        val weekly = doc ?? {
          _.values.foldLeft(0) {
            case (acc, BSONInteger(v)) => acc + v
            case (acc, _)              => acc
          }
        }
        (~daily, weekly)
      }
