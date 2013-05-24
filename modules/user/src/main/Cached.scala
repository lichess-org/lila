package lila.user

import scala.concurrent.duration._

import play.api.libs.json.JsObject

import lila.db.api.$count
import lila.memo.AsyncCache
import tube.userTube

final class Cached(ttl: Duration) {

  val username = AsyncCache(UserRepo.usernameById, maxCapacity = 50000)

  def usernameOrAnonymous(id: String): Fu[String] = 
    username(id) map (_ | User.anonymous)

  def usernameOrAnonymous(id: Option[String]): Fu[String] = 
    id.fold(fuccess(User.anonymous))(usernameOrAnonymous)

  val count = AsyncCache((o: JsObject) ⇒ $count(o), timeToLive = ttl)

  def countEnabled: Fu[Int] = count(UserRepo.enabledQuery)
}
