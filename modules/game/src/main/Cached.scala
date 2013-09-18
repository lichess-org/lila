package lila.game

import scala.concurrent.duration._

import play.api.libs.json.JsObject

import lila.db.api.$count
import lila.memo.AsyncCache
import lila.user.{ User, Confrontation }
import tube.gameTube

final class Cached(ttl: Duration) {

  def nbGames: Fu[Int] = count(Query.all)
  def nbMates: Fu[Int] = count(Query.mate)
  def nbPopular: Fu[Int] = count(Query.popular)
  def nbImported: Fu[Int] = count(Query.imported)

  def nbPlaying(userId: String): Fu[Int] = count(Query notFinished userId)

  def confrontation(user1: User, user2: User): Fu[Confrontation] = {
    def idGame(user: User) = (user.id, user.count.game)
    confrontationCache {
      (user1 < user2).fold(
        idGame(user1) -> idGame(user2),
        idGame(user2) -> idGame(user1))
    }
  }

  private val confrontationCache =
    AsyncCache(GameRepo.confrontation, timeToLive = 1.minute)

  private val count = AsyncCache((o: JsObject) ⇒ $count(o), timeToLive = ttl)
}
