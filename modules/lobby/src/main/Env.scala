package lidraughts.lobby

import akka.actor._
import com.typesafe.config.Config
import lidraughts.common.Strings
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    hub: lidraughts.hub.Env,
    onStart: String => Unit,
    blocking: String => Fu[Set[String]],
    playban: String => Fu[Option[lidraughts.playban.TempBan]],
    gameCache: lidraughts.game.Cached,
    poolApi: lidraughts.pool.PoolApi,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    settingStore: lidraughts.memo.SettingStore.Builder,
    system: ActorSystem
) {

  private val settings = new {
    val NetDomain = config getString "net.domain"
    val SocketUidTtl = config duration "socket.uid.ttl"
    val BroomPeriod = config duration "broom_period"
    val ResyncIdsPeriod = config duration "resync_ids_period"
    val CollectionSeek = config getString "collection.seek"
    val CollectionSeekArchive = config getString "collection.seek_archive"
    val SeekMaxPerPage = config getInt "seek.max_per_page"
    val SeekMaxPerUser = config getInt "seek.max_per_user"
    val MaxPlaying = config getInt "max_playing"
  }
  import settings._

  lazy val whitelistIPSetting = {
    import lidraughts.memo.SettingStore.Strings._
    settingStore[Strings](
      "whitelistIP",
      default = Strings(Nil),
      text = "IPs that are not ratelimited in the lobby - IP addresses separated by a comma".some
    )
  }

  private val socket = new LobbySocket(system, SocketUidTtl)

  lazy val seekApi = new SeekApi(
    coll = db(CollectionSeek),
    archiveColl = db(CollectionSeekArchive),
    blocking = blocking,
    asyncCache = asyncCache,
    maxPerPage = SeekMaxPerPage,
    maxPerUser = SeekMaxPerUser
  )

  private val lobbyTrouper = LobbyTrouper.start(
    system,
    broomPeriod = BroomPeriod,
    resyncIdsPeriod = ResyncIdsPeriod
  ) { () =>
    new LobbyTrouper(
      system = system,
      socket = socket,
      seekApi = seekApi,
      gameCache = gameCache,
      maxPlaying = MaxPlaying,
      blocking = blocking,
      playban = playban,
      poolApi = poolApi,
      onStart = onStart
    )
  }

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    lobby = lobbyTrouper,
    socket = socket,
    poolApi = poolApi,
    blocking = blocking
  )
  system.lidraughtsBus.subscribe(socketHandler, 'nbMembers, 'nbRounds)

  private val abortListener = new AbortListener(seekApi, lobbyTrouper)

  system.lidraughtsBus.subscribeFun('abortGame) {
    case lidraughts.game.actorApi.AbortedBy(pov) => abortListener(pov)
  }
}

object Env {

  lazy val current = "lobby" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "lobby",
    db = lidraughts.db.Env.current,
    hub = lidraughts.hub.Env.current,
    onStart = lidraughts.game.Env.current.onStart,
    blocking = lidraughts.relation.Env.current.api.fetchBlocking,
    playban = lidraughts.playban.Env.current.api.currentBan _,
    gameCache = lidraughts.game.Env.current.cached,
    poolApi = lidraughts.pool.Env.current.api,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    settingStore = lidraughts.memo.Env.current.settingStore,
    system = lidraughts.common.PlayApp.system
  )
}
