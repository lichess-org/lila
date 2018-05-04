package lila.user

import akka.actor._
import com.typesafe.config.Config

import lila.common.EmailAddress

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
    val CollectionRanking = config getString "collection.ranking"
    val PasswordBPassSecret = config getString "password.bpass.secret"
  }
  import settings._

  lazy val userColl = db(CollectionUser)

  lazy val lightUserApi = new LightUserApi(userColl)(system)

  lazy val onlineUserIdMemo = new lila.memo.ExpireSetMemo(ttl = OnlineTtl)

  lazy val noteApi = new NoteApi(db(CollectionNote), timeline, system.lilaBus)

  lazy val trophyApi = new TrophyApi(db(CollectionTrophy))

  lazy val rankingApi = new RankingApi(db(CollectionRanking), mongoCache, asyncCache, lightUser)

  lazy val jsonView = new JsonView(isOnline)

  def lightUser(id: User.ID): Fu[Option[lila.common.LightUser]] = lightUserApi async id
  def lightUserSync(id: User.ID): Option[lila.common.LightUser] = lightUserApi sync id

  def uncacheLightUser(id: User.ID): Unit = lightUserApi invalidate id

  def isOnline(userId: User.ID): Boolean = onlineUserIdMemo get userId

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.mod.MarkCheater(userId, true) => rankingApi remove userId
      case lila.hub.actorApi.mod.MarkBooster(userId) => rankingApi remove userId
      case lila.hub.actorApi.mod.KickFromRankings(userId) => rankingApi remove userId
      case User.Active(user) =>
        if (!user.seenRecently) UserRepo setSeenAt user.id
        onlineUserIdMemo put user.id
      case User.GDPRErase(user) =>
        UserRepo erase user
        noteApi erase user
    }
  })), 'adjustCheater, 'adjustBooster, 'userActive, 'kickFromRankings, 'gdprErase)

  {
    import scala.concurrent.duration._
    import lila.hub.actorApi.WithUserIds

    scheduler.effect(3 seconds, "refresh online user ids") {
      system.lilaBus.publish(WithUserIds(onlineUserIdMemo.putAll), 'users)
      onlineUserIdMemo put "lichess"
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
    timeline = lila.hub.Env.current.actor.timeline,
    system = lila.common.PlayApp.system
  )
}
