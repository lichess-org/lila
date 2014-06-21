package lila.tournament

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.hub.actorApi.map.Ask
import lila.hub.{ ActorMap, Sequencer }
import lila.socket.actorApi.GetVersion
import lila.socket.History
import makeTimeout.short

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env,
    flood: lila.security.Flood,
    hub: lila.hub.Env,
    roundMap: ActorRef,
    lightUser: String => Option[lila.common.LightUser],
    isOnline: String => Boolean,
    isDev: Boolean,
    secondsToMove: Int,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val CollectionTournament = config getString "collection.tournament"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val CreatedCacheTtl = config duration "created.cache.ttl"
    val LeaderboardCacheTtl = config duration "leaderboard.cache.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val SocketName = config getString "socket.name"
    val OrganizerName = config getString "organizer.name"
    val ReminderName = config getString "reminder.name"
    val SequencerTimeout = config duration "sequencer.timeout"
    val SequencerMapName = config getString "sequencer.map_name"
    val NetDomain = config getString "net.domain"
  }
  import settings._

  lazy val forms = new DataForm(isDev)

  lazy val api = new TournamentApi(
    sequencers = sequencerMap,
    autoPairing = autoPairing,
    router = hub.actor.router,
    renderer = hub.actor.renderer,
    timeline = hub.actor.timeline,
    socketHub = socketHub,
    site = hub.socket.site,
    lobby = hub.socket.lobby,
    roundMap = roundMap)

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketHub = socketHub,
    chat = hub.actor.chat,
    flood = flood)

  lazy val winners = new Winners(LeaderboardCacheTtl)

  private val socketHub = system.actorOf(
    Props(new lila.socket.SocketHubActor.Default[Socket] {
      def mkActor(tournamentId: String) = new Socket(
        tournamentId = tournamentId,
        history = new History(ttl = HistoryMessageTtl),
        uidTimeout = UidTimeout,
        socketTimeout = SocketTimeout,
        lightUser = lightUser)
    }), name = SocketName)

  private val sequencerMap = system.actorOf(Props(ActorMap { id =>
    new Sequencer(SequencerTimeout)
  }), name = SequencerMapName)

  private val organizer = system.actorOf(Props(new Organizer(
    api = api,
    reminder = system.actorOf(Props(new Reminder(
      renderer = hub.actor.renderer
    )), name = ReminderName),
    isOnline = isOnline,
    socketHub = socketHub,
    evaluator = hub.actor.evaluator
  )), name = OrganizerName)

  private val tournamentScheduler = system.actorOf(Props(new Scheduler(api)))

  def version(tourId: String): Fu[Int] =
    socketHub ? Ask(tourId, GetVersion) mapTo manifest[Int]

  val allCreatedSorted =
    lila.memo.AsyncCache.single(TournamentRepo.noPasswordCreatedSorted, timeToLive = CreatedCacheTtl)

  val promotable =
    lila.memo.AsyncCache.single(TournamentRepo.promotable, timeToLive = CreatedCacheTtl)

  def cli = new lila.common.Cli {
    import tube.tournamentTube
    def process = {
      case "tournament" :: "typecheck" :: Nil => lila.db.Typecheck.apply[Tournament]
      case "tournament" :: "recount" :: Nil   => api.recountAll inject "Recount done!"
    }
  }

  private lazy val autoPairing = new AutoPairing(
    roundMap = roundMap,
    system = system,
    secondsToMove = secondsToMove)

  {
    import scala.concurrent.duration._

    scheduler.message(2 seconds) {
      organizer -> actorApi.AllCreatedTournaments
    }

    scheduler.message(3 seconds) {
      organizer -> actorApi.StartedTournaments
    }

    scheduler.message(6 minutes) {
      organizer -> actorApi.CheckLeaders
    }

    scheduler.message(5 minutes) {
      tournamentScheduler -> actorApi.ScheduleNow
    }
    tournamentScheduler ! actorApi.ScheduleNow
  }

  private[tournament] lazy val tournamentColl = db(CollectionTournament)
}

object Env {

  private def hub = lila.hub.Env.current

  lazy val current = "[boot] tournament" describes new Env(
    config = lila.common.PlayApp loadConfig "tournament",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    flood = lila.security.Env.current.flood,
    hub = lila.hub.Env.current,
    roundMap = lila.round.Env.current.roundMap,
    lightUser = lila.user.Env.current.lightUser,
    isOnline = lila.user.Env.current.isOnline,
    isDev = lila.common.PlayApp.isDev,
    secondsToMove = lila.game.Env.current.MandatorySecondsToMove,
    scheduler = lila.common.PlayApp.scheduler)
}
