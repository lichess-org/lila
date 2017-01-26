package lila.game

import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.{ AsyncCache, MongoCache, ExpireSetMemo }
import lila.user.User

final class Cached(
    coll: Coll,
    mongoCache: MongoCache.Builder,
    defaultTtl: FiniteDuration) {

  def nbImportedBy(userId: String): Fu[Int] = countCache(Query imported userId)
  def clearNbImportedByCache(userId: String) = countCache.remove(Query imported userId)

  def nbPlaying(userId: String): Fu[Int] = countShortTtl(Query nowPlaying userId)

  def nbTotal: Fu[Int] = countCache($empty)

  private implicit val userHandler = User.userBSONHandler

  val rematch960 = new ExpireSetMemo(3.hours)

  val isRematch = new ExpireSetMemo(3.hours)

  private val countShortTtl = AsyncCache[Bdoc, Int](
    name = "game.countShortTtl",
    f = (o: Bdoc) => coll countSel o,
    timeToLive = 5.seconds)

  private val countCache = mongoCache(
    prefix = "game:count",
    f = (o: Bdoc) => coll countSel o,
    timeToLive = defaultTtl,
    keyToString = lila.db.BSON.hashDoc)
}
