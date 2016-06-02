package lila.game

import scala.concurrent.duration._

import chess.variant.Variant
import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument

import lila.db.BSON._
import lila.db.dsl._
import lila.memo.{ AsyncCache, MongoCache, ExpireSetMemo, Builder }
import lila.user.{ User, UidNb }
import UidNb.UidNbBSONHandler

final class Cached(
    coll: Coll,
    mongoCache: MongoCache.Builder,
    defaultTtl: FiniteDuration) {

  def nbImportedBy(userId: String): Fu[Int] = count(Query imported userId)
  def clearNbImportedByCache(userId: String) = count.remove(Query imported userId)

  def nbPlaying(userId: String): Fu[Int] = countShortTtl(Query nowPlaying userId)

  private implicit val userHandler = User.userBSONHandler

  val rematch960 = new ExpireSetMemo(3.hours)

  val isRematch = new ExpireSetMemo(3.hours)

  // very expensive
  // val activePlayerUidsDay = mongoCache[Int, List[UidNb]](
  //   prefix = "player:active:day",
  //   (nb: Int) => GameRepo.activePlayersSince(DateTime.now minusDays 1, nb),
  //   timeToLive = 1 hour)

  private val countShortTtl = AsyncCache[BSONDocument, Int](
    f = (o: BSONDocument) => coll countSel o,
    timeToLive = 5.seconds)

  private val count = mongoCache(
    prefix = "game:count",
    f = (o: BSONDocument) => coll countSel o,
    timeToLive = defaultTtl,
    keyToString = lila.db.BSON.hashDoc)
}
