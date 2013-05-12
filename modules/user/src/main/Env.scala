package lila.user

import lila.common.PimpedConfig._

import chess.EloCalculator
import com.typesafe.config.Config
import akka.actor.ActorSystem

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    system: ActorSystem,
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

  lazy val usernameMemo = new UsernameMemo(ttl = OnlineTtl)

  val forms = DataForm

  def usernameOption(id: String): Fu[Option[String]] = cached username id

  def usernameOrAnonymous(id: String): Fu[String] = cached usernameOrAnonymous id

  def setOnline(user: User) { usernameMemo put user.id }

  def isOnline(userId: String) = usernameMemo get userId

  def countEnabled = cached.countEnabled

  def cli = new lila.common.Cli {
    def process = {
      case "user" :: "average" :: "elo" :: Nil ⇒
        UserRepo.averageElo map { elo ⇒ "Average elo is %f" format elo }
    }
  }

  {
    import scala.concurrent.duration._
    import akka.pattern.{ ask, pipe }
    import makeTimeout.short
    import lila.hub.actorApi.GetUserIds

    // delay scheduler to prevent loading hub.socket.hub too early
    scheduler.once(3.seconds) {

      scheduler.effect(3 seconds, "usernameMemo: refresh") {
        hub.socket.hub ? GetUserIds mapTo manifest[Iterable[String]] foreach usernameMemo.putAll
      }
    }
  }

  private lazy val cached = new Cached(ttl = CachedNbTtl)
}

object Env {

  lazy val current: Env = "[boot] user" describes new Env(
    config = lila.common.PlayApp loadConfig "user",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
