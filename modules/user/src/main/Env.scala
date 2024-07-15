package lila.user

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.*
import lila.core.userId

@Module
final class Env(
    db: lila.db.Db,
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb,
    mongoCache: lila.memo.MongoCache.Api,
    cacheApi: lila.memo.CacheApi,
    isOnline: lila.core.socket.IsOnline,
    onlineIds: lila.core.socket.OnlineIds,
    assetBaseUrlInternal: AssetBaseUrlInternal
)(using Executor, Scheduler, akka.stream.Materializer, play.api.Mode):

  val perfsRepo = UserPerfsRepo(db(CollName("user_perf")))
  val repo      = UserRepo(db(CollName("user4")))

  val api = wire[UserApi]

  val lightUserApi: LightUserApi = wire[LightUserApi]

  export lightUserApi.{
    async as lightUser,
    asyncFallback as lightUserFallback,
    sync as lightUserSync,
    syncFallback as lightUserSyncFallback,
    isBotSync
  }

  lazy val jsonView = wire[JsonView]

  lazy val noteApi = NoteApi(repo, db(CollName("note")))

  lazy val trophyApi = TrophyApi(db(CollName("trophy")), db(CollName("trophyKind")), cacheApi)

  private lazy val rankingColl = yoloDb(CollName("ranking")).failingSilently()

  lazy val rankingApi = wire[RankingApi]

  lazy val cached: Cached                           = wire[Cached]
  def rankingsOf: UserId => lila.rating.UserRankMap = cached.rankingsOf

  lazy val forms = wire[UserForm]

  val flairApi = wire[FlairApi]
  export flairApi.{ flairOf, flairsOf }

  val flagApi: lila.core.user.FlagApi = Flags

  lila.common.Bus.subscribeFuns(
    "adjustCheater" -> { case lila.core.mod.MarkCheater(userId, true) =>
      rankingApi.remove(userId)
      repo.setRoles(userId, Nil)
    },
    "adjustBooster" -> { case lila.core.mod.MarkBooster(userId) =>
      rankingApi.remove(userId)
      repo.setRoles(userId, Nil)
    },
    "kickFromRankings" -> { case lila.core.mod.KickFromRankings(userId) =>
      rankingApi.remove(userId)
    }
  )
