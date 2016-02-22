package lila.user

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.memo.{ ExpireSetMemo, MongoCache }

final class Env(
    config: Config,
    db: lila.db.Env,
    mongoCache: MongoCache.Builder,
    scheduler: lila.common.Scheduler,
    timeline: ActorSelection,
    system: ActorSystem) {

  private val settings = new {
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val CachedNbTtl = config duration "cached.nb.ttl"
    val OnlineTtl = config duration "online.ttl"
    val CollectionUser = config getString "collection.user"
    val CollectionNote = config getString "collection.note"
    val CollectionTrophy = config getString "collection.trophy"
    val CollectionRanking = config getString "collection.ranking"
  }
  import settings._

  lazy val userColl = db(CollectionUser)

  lazy val lightUserApi = new LightUserApi(userColl)

  lazy val onlineUserIdMemo = new ExpireSetMemo(ttl = OnlineTtl)

  lazy val noteApi = new NoteApi(db(CollectionNote), timeline)

  lazy val trophyApi = new TrophyApi(db(CollectionTrophy))

  lazy val rankingApi = new RankingApi(db(CollectionRanking), mongoCache)

  lazy val jsonView = new JsonView(isOnline)

  val forms = DataForm

  def lightUser(id: String): Option[lila.common.LightUser] = lightUserApi get id

  def isOnline(userId: String) = onlineUserIdMemo get userId

  def countEnabled = cached.countEnabled

  def cli = new lila.common.Cli {
    import tube.userTube
    def process = {
      case "user" :: "typecheck" :: Nil =>
        lila.db.Typecheck.apply[User]
      case "user" :: "email" :: userId :: email :: Nil =>
        UserRepo.email(User normalize userId, email) inject "done"
    }
  }

  private val bus = system.lilaBus

  bus.subscribe(system.actorOf(
    Props(new Actor {
      def receive = {
        case User.Active(user) =>
          if (!user.seenRecently) UserRepo setSeenAt user.id
          onlineUserIdMemo put user.id
      }
    })), 'userActive)

  {
    import scala.concurrent.duration._
    import lila.hub.actorApi.WithUserIds

    scheduler.effect(3 seconds, "refresh online user ids") {
      bus.publish(WithUserIds(onlineUserIdMemo.putAll), 'users)
    }
  }

  lazy val cached = new Cached(
    nbTtl = CachedNbTtl,
    onlineUserIdMemo = onlineUserIdMemo,
    mongoCache = mongoCache,
    rankingApi = rankingApi)
}

object Env {

  lazy val current: Env = "user" boot new Env(
    config = lila.common.PlayApp loadConfig "user",
    db = lila.db.Env.current,
    mongoCache = lila.memo.Env.current.mongoCache,
    scheduler = lila.common.PlayApp.scheduler,
    timeline = lila.hub.Env.current.actor.timeline,
    system = lila.common.PlayApp.system)
}
