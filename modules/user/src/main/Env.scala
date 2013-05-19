package lila.user

import lila.common.PimpedConfig._
import lila.memo.ExpireSetMemo

import chess.EloCalculator
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    socketHub: lila.hub.ActorLazyRef,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val EloUpdaterFloor = config getInt "elo_updater.floor"
    val CachedNbTtl = config duration "cached.nb.ttl"
    val OnlineTtl = config duration "online.ttl"
    val CollectionUser = config getString "collection.user"
    val CollectionHistory = config getString "collection.history"
  }
  import settings._

  lazy val historyColl = db(CollectionHistory)

  lazy val userColl = db(CollectionUser)

  lazy val paginator = new PaginatorBuilder(
    countUsers = cached.countEnabled,
    maxPerPage = PaginatorMaxPerPage)

  lazy val eloUpdater = new EloUpdater(floor = EloUpdaterFloor)

  lazy val onlineUserIdMemo = new ExpireSetMemo(ttl = OnlineTtl)

  val forms = DataForm

  def usernameOption(id: String): Fu[Option[String]] = cached username id

  def usernameOrAnonymous(id: String): Fu[String] = cached usernameOrAnonymous id

  def setOnline(user: User) { onlineUserIdMemo put user.id }

  def isOnline(userId: String) = onlineUserIdMemo get userId

  def countEnabled = cached.countEnabled

  def cli = new lila.common.Cli {
    import tube.userTube
    def process = {
      case "user" :: "average" :: "elo" :: Nil ⇒
        UserRepo.averageElo map { elo ⇒ "Average elo is %f" format elo }
      case "user" :: "typecheck" :: Nil ⇒ lila.db.Typecheck.apply[User]
    }
  }

  {
    import scala.concurrent.duration._
    import makeTimeout.short
    import lila.hub.actorApi.WithUserIds

    scheduler.effect(3 seconds, "refresh online user ids") {
      socketHub ! WithUserIds(onlineUserIdMemo.putAll)
    }
  }

  private lazy val cached = new Cached(ttl = CachedNbTtl)
}

object Env {

  lazy val current: Env = "[boot] user" describes new Env(
    config = lila.common.PlayApp loadConfig "user",
    db = lila.db.Env.current,
    socketHub = lila.hub.Env.current.socket.hub,
    scheduler = lila.common.PlayApp.scheduler)
}
