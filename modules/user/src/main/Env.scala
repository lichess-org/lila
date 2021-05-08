package lila.user

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._

import lila.common.config._
import lila.common.LightUser
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

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    mongoCache: lila.memo.MongoCache.Api,
    cacheApi: lila.memo.CacheApi,
    isOnline: lila.socket.IsOnline,
    onlineIds: lila.socket.OnlineIds
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem,
    ws: StandaloneWSClient
) {

  private val config = appConfig.get[UserConfig]("user")(AutoConfig.loader)

  val repo = new UserRepo(db(config.collectionUser))

  val lightUserApi: LightUserApi = wire[LightUserApi]
  val lightUser                  = lightUserApi.async
  val lightUserSync              = lightUserApi.sync
  val isBotSync                  = new LightUser.IsBotSync(id => lightUserApi.sync(id).exists(_.isBot))

  lazy val botIds = new GetBotIds(() => cached.botIds.get {})

  lazy val jsonView = wire[JsonView]

  lazy val noteApi = {
    def mk = (coll: Coll) => wire[NoteApi]
    mk(db(config.collectionNote))
  }

  lazy val trophyApi = new TrophyApi(db(config.collectionTrophy), db(config.collectionTrophyKind), cacheApi)

  lazy val rankingApi = {
    def mk = (coll: Coll) => wire[RankingApi]
    mk(db(config.collectionRanking))
  }

  lazy val cached: Cached = wire[Cached]

  private lazy val passHasher = new PasswordHasher(
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
      rankingApi.remove(userId).unit
      repo.setRoles(userId, Nil).unit
    },
    "kickFromRankings" -> { case lila.hub.actorApi.mod.KickFromRankings(userId) =>
      rankingApi.remove(userId).unit
    }
  )
}
