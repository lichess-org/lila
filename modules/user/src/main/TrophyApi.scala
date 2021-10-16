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

  val kindCache = {
    // careful of collisions with trophyKindStringBSONHandler
    val trophyKindObjectBSONHandler = Macros.handler[TrophyKind]

    cacheApi.sync[String, TrophyKind](
      name = "trophy.kind",
      initialCapacity = 32,
      compute = id => kindColl.byId[TrophyKind](id)(trophyKindObjectBSONHandler).dmap(_ | TrophyKind.Unknown),
      default = _ => TrophyKind.Unknown,
      strategy = Syncache.WaitAfterUptime(20 millis),
      expireAfter = Syncache.ExpireAfterWrite(1 hour)
    )
  }

  implicit private val trophyKindStringBSONHandler =
    BSONStringHandler.as[TrophyKind](kindCache.sync, _._id)

  implicit private val trophyBSONHandler = Macros.handler[Trophy]

  def findByUser(user: User, max: Int = 50): Fu[List[Trophy]] =
    coll.list[Trophy]($doc("user" -> user.id), max).map(_.filter(_.kind != TrophyKind.Unknown))

  def roleBasedTrophies(
      user: User,
      isPublicMod: Boolean,
      isDev: Boolean,
      isVerified: Boolean,
      isContentTeam: Boolean
  ): List[Trophy] =
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
      ),
      isContentTeam option Trophy(
        _id = "",
        user = user.id,
        kind = kindCache sync TrophyKind.contentTeam,
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
