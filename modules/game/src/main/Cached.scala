package lidraughts.game

import scala.concurrent.duration._

import lidraughts.db.dsl._
import lidraughts.memo.{ MongoCache, ExpireSetMemo }
import lidraughts.user.User

final class Cached(
    coll: Coll,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    mongoCache: MongoCache.Builder
) {

  def nbImportedBy(userId: String): Fu[Int] = nbImportedCache(userId)
  def clearNbImportedByCache = nbImportedCache remove _

  def nbPlaying(userId: String): Fu[Int] = countShortTtl.get(Query nowPlaying userId)

  def nbTotal: Fu[Int] = countCache($empty)

  private implicit val userHandler = User.userBSONHandler

  private val countShortTtl = asyncCache.multi[Bdoc, Int](
    name = "game.countShortTtl",
    f = coll.countSel(_),
    expireAfter = _.ExpireAfterWrite(5.seconds)
  )

  private val nbImportedCache = mongoCache[User.ID, Int](
    prefix = "game:imported",
    f = userId => coll countSel Query.imported(userId),
    timeToLive = 1 hour,
    timeToLiveMongo = 30.days.some,
    keyToString = identity
  )

  private val countCache = mongoCache[Bdoc, Int](
    prefix = "game:count",
    f = coll.countSel(_),
    timeToLive = 1 hour,
    keyToString = lidraughts.db.BSON.hashDoc
  )
}
