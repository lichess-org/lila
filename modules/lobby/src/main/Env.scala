package lila.lobby

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.user.User

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    onStart: String => Unit,
    blocking: User.ID => Fu[Set[User.ID]],
    playban: String => Fu[Option[lila.playban.TempBan]],
    gameCache: lila.game.Cached,
    poolApi: lila.pool.PoolApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    settingStore: lila.memo.SettingStore.Builder,
    remoteSocketApi: lila.socket.RemoteSocket,
    system: ActorSystem
) {

  private val settings = new {
    val NetDomain = config getString "net.domain"
    val SocketSriTtl = config duration "socket.sri.ttl"
    val BroomPeriod = config duration "broom_period"
    val ResyncIdsPeriod = config duration "resync_ids_period"
    val CollectionSeek = config getString "collection.seek"
    val CollectionSeekArchive = config getString "collection.seek_archive"
    val SeekMaxPerPage = config getInt "seek.max_per_page"
    val SeekMaxPerUser = config getInt "seek.max_per_user"
    val MaxPlaying = config getInt "max_playing"
  }
  import settings._

  private val socket = new LobbySocket(system, SocketSriTtl)

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
  system.lilaBus.subscribe(socketHandler, 'nbMembers, 'nbRounds)

  private val remoteSocket = new LobbyRemoteSocket(
    remoteSocketApi = remoteSocketApi,
    socket = socket,
    blocking = blocking,
    controller = socketHandler.controller(socket) _,
    bus = system.lilaBus
  )

  private val abortListener = new AbortListener(seekApi, lobbyTrouper)

  import lila.memo.SettingStore.Regex._
  import lila.memo.SettingStore.Formable.regexFormable
  val socketRemoteUsersSetting = settingStore[scala.util.matching.Regex](
    "lobbySocketRemoteUsers",
    default = "".r,
    text = "Regex selecting user IDs using lobby remote socket".some
  )

  system.lilaBus.subscribeFun('abortGame) {
    case lila.game.actorApi.AbortedBy(pov) => abortListener(pov)
  }
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
    poolApi = lila.pool.Env.current.api,
    asyncCache = lila.memo.Env.current.asyncCache,
    settingStore = lila.memo.Env.current.settingStore,
    remoteSocketApi = lila.socket.Env.current.remoteSocket,
    system = lila.common.PlayApp.system
  )
}
