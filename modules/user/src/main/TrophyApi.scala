package lila.user

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.memo._
import org.joda.time.DateTime
import reactivemongo.api.bson._
import scala.concurrent.duration._

final class TrophyApi(
    coll: Coll,
    kindColl: Coll,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val trophyKindObjectBSONHandler = Macros.handler[TrophyKind]

  val kindCache = cacheApi.sync[String, TrophyKind](
    name = "trophy.kind",
    initialCapacity = 32,
    compute = id =>
      kindColl.byId(id)(trophyKindObjectBSONHandler) map { k =>
        k.getOrElse(TrophyKind.Unknown)
      },
    default = _ => TrophyKind.Unknown,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterWrite(1 hour)
  )

  implicit private val trophyKindStringBSONHandler =
    BSONStringHandler.as[TrophyKind](kindCache.sync, _._id)

  implicit private val trophyBSONHandler = Macros.handler[Trophy]

  def findByUser(user: User, max: Int = 50): Fu[List[Trophy]] =
    coll.list[Trophy]($doc("user" -> user.id), max).map(_.filter(_.kind != TrophyKind.Unknown))

  def roleBasedTrophies(user: User, isPublicMod: Boolean, isDev: Boolean, isVerified: Boolean): List[Trophy] =
    List(
      isPublicMod option Trophy(
        _id = "",
        user = user.id,
        kind = kindCache sync TrophyKind.moderator,
        date = org.joda.time.DateTime.now,
        url = none
      ),
      isDev option Trophy(
        _id = "",
        user = user.id,
        kind = kindCache sync TrophyKind.developer,
        date = org.joda.time.DateTime.now,
        url = none
      ),
      isVerified option Trophy(
        _id = "",
        user = user.id,
        kind = kindCache sync TrophyKind.verified,
        date = org.joda.time.DateTime.now,
        url = none
      )
    ).flatten

  def award(trophyUrl: String, userId: String, kindKey: String): Funit =
    coll.insert
      .one(
        $doc(
          "_id"  -> lila.common.ThreadLocalRandom.nextString(8),
          "user" -> userId,
          "kind" -> kindKey,
          "url"  -> trophyUrl,
          "date" -> DateTime.now
        )
      ) void
}
