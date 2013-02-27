package lila
package user

import memo.ActorMemo

import scala.concurrent.duration._
import scala.collection.mutable

final class Cached(userRepo: UserRepo, nbTtl: Int) {

  import Cached._

  def username(userId: String): Option[String] =
    usernameCache.getOrElseUpdate(
      userId.toLowerCase,
      (userRepo username userId).unsafePerformIO 
    )

  def usernameOrAnonymous(userId: String): String = 
    username(userId) | User.anonymous

  def usernameOrAnonymous(userId: Option[String]): String = 
    (userId flatMap username) | User.anonymous

  def countEnabled: Int = memo(CountEnabled)

  // id => username
  private val usernameCache = mutable.Map[String, Option[String]]()

  private val memo = ActorMemo(loadFromDb, nbTtl, 3.seconds)

  private def loadFromDb(key: Key) = key match {
    case CountEnabled ⇒ userRepo.countEnabled.unsafePerformIO
  }
}

object Cached {

  sealed trait Key

  case object CountEnabled extends Key
}
