package lila.tournament

import lila.socket.History
import lila.common.PimpedConfig._
import makeTimeout.short

import com.typesafe.config.Config
import akka.actor._
import akka.pattern.ask

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env,
    timelinePush: ActorRef,
    flood: lila.security.Flood,
    hub: lila.hub.Env,
    roundMeddler: lila.round.Meddler,
    getUsername: String ⇒ Fu[Option[String]],
    isDev: Boolean,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val CollectionTournament = config getString "collection.tournament"
    val CollectionRoom = config getString "collection.room"
    val MessageTtl = config duration "message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val SocketName = config getString "socket.name"
    val OrganizerName = config getString "organizer.name"
    val ReminderName = config getString "reminder.name"
    val NetDomain = config getString "net.domain"
  }
  import settings._

  lazy val forms = new DataForm(isDev)

  lazy val api = new TournamentApi(
    joiner = joiner,
    router = hub.actor.router,
    renderer = hub.actor.renderer,
    socketHub = socketHub,
    site = hub.socket.site,
    lobby = hub.socket.lobby,
    roundMeddler = roundMeddler)

  lazy val socketHandler = new SocketHandler(
    socketHub = socketHub,
    messenger = messenger,
    flood = flood)

  private lazy val history = () ⇒ new History(ttl = MessageTtl)

  private val socketHub = system.actorOf(Props(new SocketHub(
    makeHistory = history,
    messenger = messenger,
    uidTimeout = UidTimeout,
    socketTimeout = SocketTimeout,
    getUsername = getUsername,
    tournamentSocketName = name ⇒ SocketName + "-" + name
  )), name = SocketName)

  private lazy val organizer = system.actorOf(Props(new Organizer(
    api = api,
    reminder = reminder,
    socketHub = socketHub
  )), name = OrganizerName)

  private lazy val reminder = system.actorOf(Props(new Reminder(
    hub = hub.socket.hub,
    renderer = hub.actor.renderer
  )), name = ReminderName)

  def version(tourId: String): Fu[Int] =
    socketHub ? actorApi.GetTournamentVersion(tourId) mapTo manifest[Int]

  def cli = new lila.common.Cli {
    import lila.db.api.$find
    import tube.tournamentTube
    def process = {
      case "tournament" :: "typecheck" :: Nil ⇒ lila.db.Typecheck.apply[Tournament]
    }
  }

  private lazy val joiner = new GameJoiner(
    roundMeddler = roundMeddler,
    timelinePush = timelinePush,
    system = system)

  {
    import scala.concurrent.duration._

    // delay scheduler to prevent loading hub.socket.hub too early
    scheduler.once(3.seconds) {

      scheduler.message(5 seconds) {
        organizer -> actorApi.CreatedTournaments
      }

      scheduler.message(3 seconds) {
        organizer -> actorApi.StartedTournaments
      }

      scheduler.message(3 seconds) {
        organizer -> actorApi.StartPairings
      }
    }
  }

  lazy val messenger = new Messenger(NetDomain)

  private[tournament] lazy val tournamentColl = db(CollectionTournament)
  private[tournament] lazy val roomColl = db(CollectionRoom)
}

object Env {

  private def hub = lila.hub.Env.current

  lazy val current = "[boot] tournament" describes new Env(
    config = lila.common.PlayApp loadConfig "tournament",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    timelinePush = hub.actor.timeline,
    flood = lila.security.Env.current.flood,
    hub = lila.hub.Env.current,
    roundMeddler = lila.round.Env.current.meddler,
    getUsername = lila.user.Env.current.usernameOption,
    isDev = lila.common.PlayApp.isDev,
    scheduler = lila.common.PlayApp.scheduler)
}
