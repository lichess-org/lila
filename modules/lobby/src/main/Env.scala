package lila.lobby

import com.softwaremill.macwire.*
import play.api.Configuration
import scalalib.cache.ExpireSetMemo

import lila.core.config.*
import lila.core.pool.IsClockCompatible

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    onStart: lila.core.game.OnStart,
    relationApi: lila.core.relation.RelationApi,
    hasCurrentPlayban: lila.core.playban.HasCurrentPlayban,
    userApi: lila.core.user.UserApi,
    gameRepo: lila.core.game.GameRepo,
    gameApi: lila.core.game.GameApi,
    newPlayer: lila.core.game.NewPlayer,
    poolApi: lila.core.pool.PoolApi,
    cacheApi: lila.memo.CacheApi,
    socketKit: lila.core.socket.SocketKit
)(using
    Executor,
    akka.actor.ActorSystem,
    Scheduler,
    lila.core.game.IdGenerator,
    IsClockCompatible,
    lila.core.config.RateLimit
):

  private lazy val seekApiConfig = new SeekApi.Config(
    coll = db(CollName("seek")),
    archiveColl = db(CollName("seek_archive")),
    maxPerPage = MaxPerPage(13),
    maxPerUser = Max(5)
  )

  private val fixedColorLobbyCache: ExpireSetMemo[GameId] = ExpireSetMemo[GameId](2 hours)

  lazy val seekApi = wire[SeekApi]

  lazy val boardApiHookStream = wire[BoardApiHookStream]

  private lazy val lobbySyncActor = LobbySyncActor.start(
    broomPeriod = 2 seconds,
    resyncIdsPeriod = 25 seconds
  ): () =>
    wire[LobbySyncActor]

  private lazy val abortListener = wire[AbortListener]

  private lazy val biter = wire[Biter]

  val socket = wire[LobbySocket]

  lila.common.Bus.subscribeFun("abortGame"):
    case lila.core.game.AbortedBy(pov) => abortListener(pov)
