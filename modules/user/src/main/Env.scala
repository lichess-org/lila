package lila.user

import akka.actor._
import com.typesafe.config.Config

import lila.common.EmailAddress
import lila.common.PimpedConfig._

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
    val PasswordUpgradeSha = config getBoolean "password.bpass.autoupgrade"
    val PasswordParallelism = config getInt "password.bpass.parallelism"
  }
  import settings._

  lazy val userColl = db(CollectionUser)

  lazy val lightUserApi = new LightUserApi(userColl)(system)

  lazy val onlineUserIdMemo = new lila.memo.ExpireSetMemo(ttl = OnlineTtl)

  lazy val noteApi = new NoteApi(db(CollectionNote), timeline, system.lilaBus)

  lazy val trophyApi = new TrophyApi(db(CollectionTrophy))

  lazy val rankingApi = new RankingApi(db(CollectionRanking), mongoCache, asyncCache, lightUser)

  lazy val jsonView = new JsonView(isOnline)

  val forms = DataForm

  def lightUser(id: User.ID): Fu[Option[lila.common.LightUser]] = lightUserApi async id
  def lightUserSync(id: User.ID): Option[lila.common.LightUser] = lightUserApi sync id

  def uncacheLightUser(id: User.ID): Unit = lightUserApi invalidate id

  def isOnline(userId: User.ID): Boolean = onlineUserIdMemo get userId

  def cli = new lila.common.Cli {
    def process = {
      case "user" :: "email" :: userId :: email :: Nil =>
        UserRepo.email(User normalize userId, EmailAddress(email)) inject "done"
    }
  }

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lila.hub.actorApi.mod.MarkCheater(userId, true) => rankingApi remove userId
      case lila.hub.actorApi.mod.MarkBooster(userId) => rankingApi remove userId
      case lila.hub.actorApi.mod.KickFromRankings(userId) => rankingApi remove userId
      case User.Active(user) =>
        if (!user.seenRecently) UserRepo setSeenAt user.id
        onlineUserIdMemo put user.id
    }
  })), 'adjustCheater, 'adjustBooster, 'userActive, 'kickFromRankings)

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

  lazy val passwordAuth = new Authenticator(
    new AsyncPasswordHasher(
      new PasswordHasher(
        secret = PasswordBPassSecret,
        logRounds = 10,
        hashTimer = lila.mon.measure(_.user.auth.hashTime)
      ),
      new lila.hub.SyncMultiSequencer(
        system = system,
        parallelismFactor = PasswordParallelism
      )
    ),
    lila.mon.user.auth.shaLogin()
  )

  lazy val upgradeShaPasswords = PasswordUpgradeSha
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
