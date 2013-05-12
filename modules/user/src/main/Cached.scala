package lila.user

import scala.concurrent.duration._

import spray.caching.{ LruCache, Cache }

final class Cached(ttl: Duration) {

  def username(id: String): Fu[Option[String]] =
    usernameCache.fromFuture(id)(UserRepo usernameById id)

  def usernameOrAnonymous(id: String): Fu[String] = 
    username(id) map (_ | User.anonymous)

  def usernameOrAnonymous(id: Option[String]): Fu[String] = 
    id.fold(fuccess(User.anonymous))(usernameOrAnonymous)

  def countEnabled: Fu[Int] = countEnabledCache.fromFuture(true)(UserRepo.countEnabled)

  // id => username
  private val usernameCache: Cache[Option[String]] = LruCache(maxCapacity = 99999)

  private val countEnabledCache: Cache[Int] = LruCache(timeToLive = ttl)
}
