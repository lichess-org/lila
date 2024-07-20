package lila.user

import reactivemongo.api.bson.*
import scalalib.ThreadLocalRandom

import lila.db.dsl.{ *, given }
import lila.memo.*

final class TrophyApi(
    coll: Coll,
    kindColl: Coll,
    cacheApi: CacheApi
)(using Executor)
    extends lila.core.user.TrophyApi:

  val kindCache = cacheApi.sync[String, TrophyKind](
    name = "trophy.kind",
    initialCapacity = 32,
    compute = id =>
      given BSONDocumentReader[TrophyKind] = Macros.reader[TrophyKind]
      kindColl.byId[TrophyKind](id).dmap(_ | TrophyKind.Unknown)
    ,
    default = _ => TrophyKind.Unknown,
    strategy = Syncache.Strategy.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfter.Write(1 hour)
  )

  private given BSONHandler[TrophyKind]     = BSONStringHandler.as[TrophyKind](kindCache.sync, _._id)
  private given BSONDocumentHandler[Trophy] = Macros.handler[Trophy]

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
      isPublicMod.option(
        Trophy(
          _id = "",
          user = user.id,
          kind = kindCache.sync(TrophyKind.moderator),
          date = nowInstant,
          url = none
        )
      ),
      isDev.option(
        Trophy(
          _id = "",
          user = user.id,
          kind = kindCache.sync(TrophyKind.developer),
          date = nowInstant,
          url = none
        )
      ),
      isVerified.option(
        Trophy(
          _id = "",
          user = user.id,
          kind = kindCache.sync(TrophyKind.verified),
          date = nowInstant,
          url = none
        )
      ),
      isContentTeam.option(
        Trophy(
          _id = "",
          user = user.id,
          kind = kindCache.sync(TrophyKind.contentTeam),
          date = nowInstant,
          url = none
        )
      )
    ).flatten

  def award(trophyUrl: String, userId: UserId, kindKey: String): Funit =
    coll.insert
      .one(
        $doc(
          "_id"  -> ThreadLocalRandom.nextString(8),
          "user" -> userId,
          "kind" -> kindKey,
          "url"  -> trophyUrl,
          "date" -> nowInstant
        )
      ) void
