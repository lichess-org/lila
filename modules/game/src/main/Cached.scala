package lila.game

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json.JsObject

import lila.db.api.$count
import lila.memo.{ AsyncCache, ExpireSetMemo }
import tube.gameTube

final class Cached(ttl: Duration) {

  def nbGames: Fu[Int] = count(Query.all)
  def nbMates: Fu[Int] = count(Query.mate)
  def nbImported: Fu[Int] = count(Query.imported)

  def nbPlaying(userId: String): Fu[Int] = count(Query notFinished userId)

  val rematch960 = new ExpireSetMemo(3.hours)

  val activePlayerUidsDay = AsyncCache(
    (nb: Int) => GameRepo.activePlayersSince(DateTime.now minusDays 1, nb),
    timeToLive = 1 hour)

  val activePlayerUidsWeek = AsyncCache(
    (nb: Int) => GameRepo.activePlayersSince(DateTime.now minusWeeks 1, nb),
    timeToLive = 6 hours)

  private val count = AsyncCache((o: JsObject) => $count(o), timeToLive = ttl)
}
