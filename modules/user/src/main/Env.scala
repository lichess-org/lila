package lila.user

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.*
import lila.core.userId

import lila.common.Bus

@Module
final class Env(
    db: lila.db.Db,
    getFile: lila.common.config.GetRelativeFile,
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb,
    mongoCache: lila.memo.MongoCache.Api,
    cacheApi: lila.memo.CacheApi,
    isOnline: lila.core.socket.IsOnline,
    onlineIds: lila.core.socket.OnlineIds
)(using Executor, Scheduler):

  val perfsRepo = UserPerfsRepo(db(CollName("user_perf")))
  val repo = UserRepo(db(CollName("user4")))

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

  lazy val noteApi = NoteApi(db(CollName("note")))

  lazy val trophyApi = TrophyApi(db(CollName("trophy")), db(CollName("trophyKind")), cacheApi)

  private lazy val rankingColl = yoloDb(CollName("ranking")).failingSilently()

  lazy val rankingApi = wire[RankingApi]

  lazy val cached: Cached = wire[Cached]
  def rankingsOf: UserId => lila.rating.UserRankMap = cached.rankingsOf

  lazy val forms = wire[UserForm]

  val flairApi = wire[FlairApi]
  export flairApi.{ flairOf, flairsOf }

  val flagApi: lila.core.user.FlagApi = Flags

  Bus.sub[lila.core.mod.MarkCheater]:
    case lila.core.mod.MarkCheater(userId, true) =>
      rankingApi.remove(userId)
      repo.setRoles(userId, Nil)

  Bus.sub[lila.core.mod.MarkBooster]: m =>
    rankingApi.remove(m.userId)
    repo.setRoles(m.userId, Nil)

  Bus.sub[lila.core.mod.KickFromRankings]: k =>
    rankingApi.remove(k.userId)

  Bus.sub[lila.core.misc.puzzle.StreakRun]: r =>
    api.addPuzRun("streak", r.userId, r.score)
