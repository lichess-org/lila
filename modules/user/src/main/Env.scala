package lila.user

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.common.config._
import lila.common.{ CollName, Secret, MaxPerPage }

case class UserConfig(
    @ConfigName("paginator.max_per_page") paginatorMaxPerPage: MaxPerPage,
    @ConfigName("cached.nb.ttl") cachedNbTtl: FiniteDuration,
    @ConfigName("online.ttl") onlineTtl: FiniteDuration,
    @ConfigName("collection.user") collectionUser: CollName,
    @ConfigName("collection.note") collectionNote: CollName,
    @ConfigName("collection.trophy") collectionTrophy: CollName,
    @ConfigName("collection.trophyKind") collectionTrophyKind: CollName,
    @ConfigName("collection.ranking") collectionRanking: CollName,
    @ConfigName("password.bpass.secret") passwordBPassSecret: Secret
)

final class Env(
    appConfig: Configuration,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
    asyncCache: lila.memo.AsyncCache.Builder,
    scheduler: Scheduler,
    timeline: ActorSelection,
    onlineUserIds: () => Set[User.ID]
)(implicit system: ActorSystem, ws: WSClient) {

  private val config = appConfig.get[UserConfig]("user")(AutoConfig.loader)

  val userRepo = new UserRepo(db(config.collectionUser))

  val lightUserApi = wire[LightUserApi]

  private def isOnline(userId: User.ID): Boolean = onlineUserIds() contains userId

  val jsonView = new JsonView(isOnline)

  lazy val noteApi = new NoteApi(userRepo, db(config.collectionNote), timeline)

  lazy val trophyApi = new TrophyApi(db(config.collectionTrophy), db(config.collectionTrophyKind))(system)

  lazy val rankingApi = new RankingApi(userRepo, db(config.collectionRanking), mongoCache, lightUser)(system)

  def lightUser(id: User.ID): Fu[Option[lila.common.LightUser]] = lightUserApi async id
  def lightUserSync(id: User.ID): Option[lila.common.LightUser] = lightUserApi sync id

  def uncacheLightUser(id: User.ID): Unit = lightUserApi invalidate id

  system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
    lightUserApi.monitorCache
  }

  lila.common.Bus.subscribeFuns(
    "adjustCheater" -> {
      case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
        rankingApi remove userId
        userRepo.setRoles(userId, Nil)
    },
    "adjustBooster" -> {
      case lila.hub.actorApi.mod.MarkBooster(userId) => rankingApi remove userId
    },
    "kickFromRankings" -> {
      case lila.hub.actorApi.mod.KickFromRankings(userId) => rankingApi remove userId
    },
    "gdprErase" -> {
      case User.GDPRErase(user) =>
        userRepo erase user
        noteApi erase user
    }
  )

  def makeCached = (nbTtl: FiniteDuration) => wire[Cached]

  lazy val cached: Cached = makeCached(config.cachedNbTtl)

  lazy val authenticator = new Authenticator(
    passHasher = new PasswordHasher(
      secret = config.passwordBPassSecret,
      logRounds = 10,
      hashTimer = res => {
        lila.mon.measure(_.user.auth.hashTime) {
          lila.mon.measureIncMicros(_.user.auth.hashTimeInc)(res)
        }
      }
    ),
    userRepo = userRepo
  )

  lazy val forms = new DataForm(authenticator)
}
