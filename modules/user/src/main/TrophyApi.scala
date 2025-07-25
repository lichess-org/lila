package lila.user

import reactivemongo.api.bson.*
import scalalib.ThreadLocalRandom

import lila.db.dsl.{ *, given }
import lila.memo.*
import lila.core.perm.Granter

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
    strategy = Syncache.Strategy.WaitAfterUptime(20.millis),
    expireAfter = Syncache.ExpireAfter.Write(1.hour)
  )

  private given BSONHandler[TrophyKind] = BSONStringHandler.as[TrophyKind](kindCache.sync, _._id)
  private given BSONDocumentHandler[Trophy] = Macros.handler[Trophy]

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    coll.delete.one($doc("user" -> del.id))

  def findByUser(user: User, max: Int = 50): Fu[List[Trophy]] =
    coll.list[Trophy]($doc("user" -> user.id), max).map(_.filter(_.kind != TrophyKind.Unknown))

  def roleBasedTrophies(user: User): List[Trophy] =
    List(
      Granter
        .ofUser(_.PublicMod)(user)
        .option:
          Trophy(
            _id = "",
            user = user.id,
            kind = kindCache.sync(TrophyKind.moderator),
            date = nowInstant,
            url = none
          )
      ,
      Granter
        .ofUser(_.Developer)(user)
        .option:
          Trophy(
            _id = "",
            user = user.id,
            kind = kindCache.sync(TrophyKind.developer),
            date = nowInstant,
            url = none
          )
      ,
      Granter
        .ofUser(_.Verified)(user)
        .option:
          Trophy(
            _id = "",
            user = user.id,
            kind = kindCache.sync(TrophyKind.verified),
            date = nowInstant,
            url = none
          )
      ,
      Granter
        .ofUser(_.ContentTeam)(user)
        .option:
          Trophy(
            _id = "",
            user = user.id,
            kind = kindCache.sync(TrophyKind.contentTeam),
            date = nowInstant,
            url = none
          )
      ,
      Granter
        .ofUser(_.BroadcastTeam)(user)
        .option:
          Trophy(
            _id = "",
            user = user.id,
            kind = kindCache.sync(TrophyKind.broadcastTeam),
            date = nowInstant,
            url = none
          )
    ).flatten

  def award(trophyUrl: String, userId: UserId, kindKey: String): Funit =
    coll.insert
      .one(
        $doc(
          "_id" -> ThreadLocalRandom.nextString(8),
          "user" -> userId,
          "kind" -> kindKey,
          "url" -> trophyUrl,
          "date" -> nowInstant
        )
      )
      .void
