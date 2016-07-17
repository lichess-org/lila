package lila.lobby

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.socket.History

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    onStart: String => Unit,
    blocking: String => Fu[Set[String]],
    playban: String => Fu[Option[lila.playban.TempBan]],
    gameCache: lila.game.Cached,
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val MessageTtl = config duration "message.ttl"
    val NetDomain = config getString "net.domain"
    val SocketName = config getString "socket.name"
    val SocketUidTtl = config duration "socket.uid.ttl"
    val OrphanHookTtl = config duration "orphan_hook.ttl"
    val ActorName = config getString "actor.name"
    val BroomPeriod = config duration "broom_period"
    val ResyncIdsPeriod = config duration "resync_ids_period"
    val CollectionSeek = config getString "collection.seek"
    val CollectionSeekArchive = config getString "collection.seek_archive"
    val SeekMaxPerPage = config getInt "seek.max_per_page"
    val SeekMaxPerUser = config getInt "seek.max_per_user"
    val MaxPlaying = config getInt "max_playing"
  }
  import settings._

  private val socket = system.actorOf(Props(new Socket(
    history = history,
    uidTtl = SocketUidTtl)), name = SocketName)

  lazy val seekApi = new SeekApi(
    coll = db(CollectionSeek),
    archiveColl = db(CollectionSeekArchive),
    blocking = blocking,
    maxPerPage = SeekMaxPerPage,
    maxPerUser = SeekMaxPerUser)

  val lobby = Lobby.start(system, ActorName,
    broomPeriod = BroomPeriod,
    resyncIdsPeriod = ResyncIdsPeriod) {
      new Lobby(
        socket = socket,
        seekApi = seekApi,
        gameCache = gameCache,
        maxPlaying = MaxPlaying,
        blocking = blocking,
        playban = playban,
        onStart = onStart)
    }

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    lobby = lobby,
    socket = socket,
    blocking = blocking)

  lazy val history = new History[actorApi.Messadata](ttl = MessageTtl)

  private val abortListener = new AbortListener(seekApi = seekApi)

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lila.game.actorApi.AbortedBy(pov) => abortListener(pov)
    }
  })), 'abortGame)
}

object Env {

  lazy val current = "lobby" boot new Env(
    config = lila.common.PlayApp loadConfig "lobby",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    onStart = lila.game.Env.current.onStart,
    blocking = lila.relation.Env.current.api.fetchBlocking,
    playban = lila.playban.Env.current.api.currentBan _,
    gameCache = lila.game.Env.current.cached,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
