package lila.team

import lila.user.User

import spray.caching.{ LruCache, Cache }
import play.api.libs.concurrent.Execution.Implicits._

final class Cached(capacity: Int) {

  object name {

    private val cache: Cache[Option[String]] = LruCache(maxCapacity = capacity)

    def apply(id: String): Fu[Option[String]] =
      cache.fromFuture(id)(TeamRepo name id)
  }

  object teamIds {

    private val cache: Cache[List[String]] = LruCache(maxCapacity = capacity)

    def apply(userId: String): Fu[List[String]] =
      cache.fromFuture(userId)(MemberRepo teamIdsByUser userId)

    def invalidate(userId: String): Funit = cache.remove(userId) zmap (_.void)
  }

  object nbRequests {

    private val cache: Cache[Int] = LruCache(maxCapacity = capacity)

    def apply(userId: String): Fu[Int] =
      cache.fromFuture(userId) {
        TeamRepo teamIdsByCreator userId flatMap RequestRepo.countByTeams
      }

    def invalidate(userId: String): Funit = cache.remove(userId) zmap (_.void)
  }
}
