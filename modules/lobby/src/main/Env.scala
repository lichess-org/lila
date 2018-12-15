package lila.lobby

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    onStart: String => Unit,
    blocking: String => Fu[Set[String]],
    playban: String => Fu[Option[lila.playban.TempBan]],
    gameCache: lila.game.Cached,
    poolApi: lila.pool.PoolApi,
    asyncCache: lila.memo.AsyncCache.Builder,
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
  system.lilaBus.subscribe(socketHandler, 'nbMembers, 'nbRounds)

  private val abortListener = new AbortListener(seekApi, lobbyTrouper)

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
    system = lila.common.PlayApp.system
  )
}
