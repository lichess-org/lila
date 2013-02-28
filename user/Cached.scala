package lila.user

import lila.common.Futuristic

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import spray.caching.{ LruCache, Cache }
import spray.util._

import scala.collection.mutable

final class Cached(userRepo: UserRepo, ttl: Duration) extends Futuristic {

  def username(userId: String): Option[String] =
    usernameCache.getOrElseUpdate(
      userId.toLowerCase,
      (userRepo username userId).unsafePerformIO
    )

  def usernameOrAnonymous(userId: String): String =
    username(userId) | User.anonymous

  def usernameOrAnonymous(userId: Option[String]): String =
    (userId flatMap username) | User.anonymous

  def countEnabled: Int = 
    cache.fromFuture("count-enabled")(userRepo.countEnabled.toFuture).await

  // id => username
  private val usernameCache = mutable.Map[String, Option[String]]()

  private val cache: Cache[Int] = LruCache(timeToLive = ttl)
}
