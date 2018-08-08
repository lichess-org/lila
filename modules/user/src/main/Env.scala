package lidraughts.user

import akka.actor._
import com.typesafe.config.Config

import lidraughts.common.EmailAddress

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    mongoCache: lidraughts.memo.MongoCache.Builder,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    scheduler: lidraughts.common.Scheduler,
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
    val CollectionRanking = config getString "collection.ranking"
    val PasswordBPassSecret = config getString "password.bpass.secret"
  }
  import settings._

  lazy val userColl = db(CollectionUser)

  lazy val lightUserApi = new LightUserApi(userColl)(system)

  lazy val onlineUserIdMemo = new lidraughts.memo.ExpireSetMemo(ttl = OnlineTtl)

  lazy val noteApi = new NoteApi(db(CollectionNote), timeline, system.lidraughtsBus)

  lazy val trophyApi = new TrophyApi(db(CollectionTrophy))

  lazy val rankingApi = new RankingApi(db(CollectionRanking), mongoCache, asyncCache, lightUser)

  lazy val jsonView = new JsonView(isOnline)

  def lightUser(id: User.ID): Fu[Option[lidraughts.common.LightUser]] = lightUserApi async id
  def lightUserSync(id: User.ID): Option[lidraughts.common.LightUser] = lightUserApi sync id

  def uncacheLightUser(id: User.ID): Unit = lightUserApi invalidate id

  def isOnline(userId: User.ID): Boolean = onlineUserIdMemo get userId

  system.lidraughtsBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lidraughts.hub.actorApi.mod.MarkCheater(userId, true) => rankingApi remove userId
      case lidraughts.hub.actorApi.mod.MarkBooster(userId) => rankingApi remove userId
      case lidraughts.hub.actorApi.mod.KickFromRankings(userId) => rankingApi remove userId
      case User.Active(user) =>
        if (!user.seenRecently) UserRepo setSeenAt user.id
        onlineUserIdMemo put user.id
    }
  })), 'adjustCheater, 'adjustBooster, 'userActive, 'kickFromRankings)

  {
    import scala.concurrent.duration._
    import lidraughts.hub.actorApi.WithUserIds

    scheduler.effect(3 seconds, "refresh online user ids") {
      system.lidraughtsBus.publish(WithUserIds(onlineUserIdMemo.putAll), 'users)
      onlineUserIdMemo put "lidraughts"
    }
  }

  lazy val cached = new Cached(
    userColl = userColl,
    nbTtl = CachedNbTtl,
    onlineUserIdMemo = onlineUserIdMemo,
    mongoCache = mongoCache,
    asyncCache = asyncCache,
    rankingApi = rankingApi
  )

  lazy val authenticator = new Authenticator(
    passHasher = new PasswordHasher(
      secret = PasswordBPassSecret,
      logRounds = 10,
      hashTimer = res => {
        lidraughts.mon.measure(_.user.auth.hashTime) {
          lidraughts.mon.measureIncMicros(_.user.auth.hashTimeInc)(res)
        }
      }
    ),
    userRepo = UserRepo
  )

  lazy val forms = new DataForm(authenticator)
}

object Env {

  lazy val current: Env = "user" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "user",
    db = lidraughts.db.Env.current,
    mongoCache = lidraughts.memo.Env.current.mongoCache,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    scheduler = lidraughts.common.PlayApp.scheduler,
    timeline = lidraughts.hub.Env.current.actor.timeline,
    system = lidraughts.common.PlayApp.system
  )
}
