package lila.user

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.*
import lila.db.dsl.Coll

private class UserConfig(
    @ConfigName("online.ttl") val onlineTtl: FiniteDuration,
    @ConfigName("collection.user") val collectionUser: CollName,
    @ConfigName("collection.note") val collectionNote: CollName,
    @ConfigName("collection.trophy") val collectionTrophy: CollName,
    @ConfigName("collection.trophyKind") val collectionTrophyKind: CollName,
    @ConfigName("collection.ranking") val collectionRanking: CollName,
    @ConfigName("password.bpass.secret") val passwordBPassSecret: Secret
)

object A:
  object B:
    val foo = "bar"

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb,
    mongoCache: lila.memo.MongoCache.Api,
    cacheApi: lila.memo.CacheApi,
    isOnline: lila.socket.IsOnline,
    onlineIds: lila.socket.OnlineIds
)(using
    ec: Executor,
    scheduler: Scheduler,
    ws: StandaloneWSClient
):

  private val config = appConfig.get[UserConfig]("user")(AutoConfig.loader)

  val repo = UserRepo(db(config.collectionUser))

  val lightUserApi: LightUserApi = wire[LightUserApi]

  export lightUserApi.{
    async as lightUser,
    asyncFallback as lightUserFallback,
    sync as lightUserSync,
    syncFallback as lightUserSyncFallback,
    isBotSync
  }

  lazy val botIds     = GetBotIds(() => cached.botIds.get {})
  lazy val rankingsOf = RankingsOf(cached.rankingsOf)

  lazy val jsonView = wire[JsonView]

  lazy val noteApi =
    def mk = (coll: Coll) => wire[NoteApi]
    mk(db(config.collectionNote))

  lazy val trophyApi = TrophyApi(db(config.collectionTrophy), db(config.collectionTrophyKind), cacheApi)

  private lazy val rankingColl = yoloDb(config.collectionRanking).failingSilently()

  lazy val rankingApi = wire[RankingApi]

  lazy val cached: Cached = wire[Cached]

  private lazy val passHasher = PasswordHasher(
    secret = config.passwordBPassSecret,
    logRounds = 10,
    hashTimer = res => lila.common.Chronometer.syncMon(_.user.auth.hashTime)(res)
  )

  lazy val authenticator = wire[Authenticator]

  lazy val forms = wire[UserForm]

  lila.common.Bus.subscribeFuns(
    "adjustCheater" -> { case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
      rankingApi remove userId
      repo.setRoles(userId, Nil).unit
    },
    "adjustBooster" -> { case lila.hub.actorApi.mod.MarkBooster(userId) =>
      rankingApi remove userId
      repo.setRoles(userId, Nil).unit
    },
    "kickFromRankings" -> { case lila.hub.actorApi.mod.KickFromRankings(userId) =>
      rankingApi.remove(userId).unit
    }
  )
