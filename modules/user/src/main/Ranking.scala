package lila.user

import scala.concurrent.duration._

import play.api.libs.json.Json
import org.joda.time.DateTime

import lila.db.api._
import lila.db.Implicits._
import lila.memo.AsyncCache
import lila.rating.Glicko
import tube.userTube

private[user] final class Ranking(ttl: Duration) {

  def get(id: String): Fu[Option[Int]] = cache(true) map (_ get id)

  private val cache = AsyncCache.single(compute, timeToLive = ttl)

  private def compute: Fu[Map[String, Int]] =
    $primitive(
      UserRepo.stableGoodLadSelect ++
        UserRepo.perfSince("global", maxInactivityDate) ++
        Json.obj("rating" -> $gt(Glicko.default.intRating)),
      "_id",
      _ sort UserRepo.sortRatingDesc
    )(_.asOpt[String]) map { _.zipWithIndex.map(x => x._1 -> (x._2 + 1)).toMap }
}
