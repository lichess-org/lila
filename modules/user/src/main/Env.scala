package lila.user

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.hub.actorApi.socket.WithUserIds

final class Env(
    config: Config,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
    asyncCache: lila.memo.AsyncCache.Builder,
    scheduler: lila.common.Scheduler,
    timeline: ActorSelection,
    system: ActorSystem
) {

  private val settings = new {
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val CachedNbTtl = config duration "cached.nb.ttl"
    val OnlineTtl = config duration "online.ttl"
    val CollectionUser = config getString "collection.user"
    val CollectionNote = config getString "collection.note"
    val CollectionTrophy = config getString "collection.trophy"
    val CollectionTrophyKind = config getString "collection.trophyKind"
    val CollectionRanking = config getString "collection.ranking"
    val PasswordBPassSecret = config getString "password.bpass.secret"
  }
  import settings._

  val userColl = db(CollectionUser)

  val lightUserApi = new LightUserApi(userColl)(system)

  val onlineUserIdMemo = new lila.memo.ExpireSetMemo(ttl = OnlineTtl)
  val recentTitledUserIdMemo = new lila.memo.ExpireSetMemo(ttl = 3 hours)

  def isOnline(userId: User.ID): Boolean = onlineUserIdMemo get userId

  val jsonView = new JsonView(isOnline)

  lazy val noteApi = new NoteApi(db(CollectionNote), timeline, system.lilaBus)

  lazy val trophyApi = new TrophyApi(db(CollectionTrophy), db(CollectionTrophyKind))(system)

  lazy val rankingApi = new RankingApi(db(CollectionRanking), mongoCache, asyncCache, lightUser)

  def lightUser(id: User.ID): Fu[Option[lila.common.LightUser]] = lightUserApi async id
  def lightUserSync(id: User.ID): Option[lila.common.LightUser] = lightUserApi sync id

  def uncacheLightUser(id: User.ID): Unit = lightUserApi invalidate id

  system.scheduler.schedule(1 minute, 1 minute) {
    lightUserApi.monitorCache
  }

  system.lilaBus.subscribeFuns(
    'adjustCheater -> {
      case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
        rankingApi remove userId
        UserRepo.setRoles(userId, Nil)
    },
    'adjustBooster -> {
      case lila.hub.actorApi.mod.MarkBooster(userId) => rankingApi remove userId
    },
    'userActive -> {
      case User.Active(user) =>
        if (!user.seenRecently) UserRepo setSeenAt user.id
        onlineUserIdMemo put user.id
        if (user.hasTitle) recentTitledUserIdMemo put user.id
    },
    'kickFromRankings -> {
      case lila.hub.actorApi.mod.KickFromRankings(userId) => rankingApi remove userId
    },
    'gdprErase -> {
      case User.GDPRErase(user) =>
        UserRepo erase user
        noteApi erase user
    }
  )

  scheduler.effect(3 seconds, "refresh online user ids") {
    system.lilaBus.publish(WithUserIds(onlineUserIdMemo.putAll), 'socketUsers)
    onlineUserIdMemo put User.lichessId
  }

  lazy val cached = new Cached(
    userColl = userColl,
    nbTtl = CachedNbTtl,
    onlineUserIdMemo = onlineUserIdMemo,
    mongoCache = mongoCache,
    asyncCache = asyncCache,
    rankingApi = rankingApi
  )(system)

  lazy val authenticator = new Authenticator(
    passHasher = new PasswordHasher(
      secret = PasswordBPassSecret,
      logRounds = 10,
      hashTimer = res => {
        lila.mon.measure(_.user.auth.hashTime) {
          lila.mon.measureIncMicros(_.user.auth.hashTimeInc)(res)
        }
      }
    ),
    userRepo = UserRepo
  )

  lazy val forms = new DataForm(authenticator)
}

object Env {

  lazy val current: Env = "user" boot new Env(
    config = lila.common.PlayApp loadConfig "user",
    db = lila.db.Env.current,
    mongoCache = lila.memo.Env.current.mongoCache,
    asyncCache = lila.memo.Env.current.asyncCache,
    scheduler = lila.common.PlayApp.scheduler,
    timeline = lila.hub.Env.current.timeline,
    system = lila.common.PlayApp.system
  )
}
