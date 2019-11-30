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
    remoteSocketApi: lila.socket.RemoteSocket,
    system: ActorSystem
) {

  private val settings = new {
    val NetDomain = config getString "net.domain"
    val ResyncIdsPeriod = config duration "resync_ids_period"
    val CollectionSeek = config getString "collection.seek"
    val CollectionSeekArchive = config getString "collection.seek_archive"
    val SeekMaxPerPage = config getInt "seek.max_per_page"
    val SeekMaxPerUser = config getInt "seek.max_per_user"
    val MaxPlaying = config getInt "max_playing"
  }
  import settings._

  lazy val seekApi = new SeekApi(
    coll = db(CollectionSeek),
    archiveColl = db(CollectionSeekArchive),
    blocking = blocking,
    asyncCache = asyncCache,
    maxPerPage = SeekMaxPerPage,
    maxPerUser = SeekMaxPerUser
  )

  private val lobbyTrouper = LobbyTrouper.start(
    broomPeriod = 2.seconds,
    resyncIdsPeriod = ResyncIdsPeriod
  ) { () =>
    new LobbyTrouper(
      system = system,
      seekApi = seekApi,
      gameCache = gameCache,
      maxPlaying = MaxPlaying,
      blocking = blocking,
      playban = playban,
      poolApi = poolApi,
      onStart = onStart
    )
  }(system)

  private val remoteSocket: LobbySocket = new LobbySocket(
    remoteSocketApi = remoteSocketApi,
    lobby = lobbyTrouper,
    blocking = blocking,
    poolApi = poolApi,
    system = system
  )

  private val abortListener = new AbortListener(seekApi, lobbyTrouper)

  lila.common.Bus.subscribeFun("abortGame") {
    case lila.game.actorApi.AbortedBy(pov) => abortListener(pov)
  }
}

object Env {

  lazy val current = "lobby" boot new Env(
    config = lila.common.PlayApp loadConfig "lobby",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    onStart = lila.round.Env.current.onStart,
    blocking = lila.relation.Env.current.api.fetchBlocking,
    playban = lila.playban.Env.current.api.currentBan _,
    gameCache = lila.game.Env.current.cached,
    poolApi = lila.pool.Env.current.api,
    asyncCache = lila.memo.Env.current.asyncCache,
    remoteSocketApi = lila.socket.Env.current.remoteSocket,
    system = lila.common.PlayApp.system
  )
}
