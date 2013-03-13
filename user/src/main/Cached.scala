package lila.user

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.collection.mutable

import spray.caching.{ LruCache, Cache }
import spray.util._

import play.api.libs.concurrent.Execution.Implicits._

final class Cached(userRepo: UserRepo, ttl: Duration) {

  def username(id: String): Option[String] =
    usernameCache.getOrElseUpdate(id.toLowerCase, (userRepo usernameById id).await)

  def usernameOrAnonymous(id: String): String = username(id) | Users.anonymous

  def usernameOrAnonymous(id: Option[String]): String = (id flatMap username) | Users.anonymous

  def countEnabled: Fu[Int] = cache.fromFuture("count-enabled")(userRepo.countEnabled)

  // id => username
  private val usernameCache = mutable.Map[String, Option[String]]()

  private val cache: Cache[Int] = LruCache(timeToLive = ttl)
}
