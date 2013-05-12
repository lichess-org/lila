package lila.tournament

import lila.socket.History

import com.typesafe.config.Config
import lila.common.PimpedConfig._
import akka.actor._

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env,
    timelinePush: ActorRef,
    flood: lila.security.Flood,
    siteSocket: ActorRef,
    lobbySocket: ActorRef,
    hubSocket: ActorRef,
    roundMeddler: lila.round.Meddler,
    getUsername: String ⇒ Fu[Option[String]],
    router: ActorRef,
    renderer: ActorRef,
    isDev: Boolean) {

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
    router = router,
    renderer = renderer,
    socketHub = socketHub,
    site = siteSocket,
    lobby = lobbySocket,
    roundMeddler = roundMeddler)

  lazy val socketHandler = new SocketHandler(
    socketHub = socketHub,
    messenger = messenger,
    flood = flood)

  lazy val history = () ⇒ new History(ttl = MessageTtl)

  lazy val socketHub = system.actorOf(Props(new SocketHub(
    makeHistory = history,
    messenger = messenger,
    uidTimeout = UidTimeout,
    socketTimeout = SocketTimeout,
    getUsername = getUsername,
    tournamentSocketName = name ⇒ SocketName + "-" + name
  )), name = SocketName)

  lazy val organizer = system.actorOf(Props(new Organizer(
    api = api,
    reminder = reminder,
    socketHub = socketHub
  )), name = OrganizerName)

  lazy val reminder = system.actorOf(Props(new Reminder(
    hub = hubSocket,
    renderer = renderer
  )), name = ReminderName)

  private lazy val joiner = new GameJoiner(
    roundMeddler = roundMeddler,
    timelinePush = timelinePush,
    system = system)

  private[tournament] lazy val messenger = new Messenger(NetDomain)

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
    siteSocket = hub.socket.site,
    lobbySocket = hub.socket.lobby,
    hubSocket = hub.socket.hub,
    roundMeddler = lila.round.Env.current.meddler,
    getUsername = lila.user.Env.current.usernameOption,
    router = hub.actor.router,
    renderer = hub.actor.renderer,
    isDev = lila.common.PlayApp.isDev)
}
