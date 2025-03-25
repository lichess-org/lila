package lila.lobby

import com.softwaremill.macwire.*

import lila.core.config.*
import lila.core.pool.IsClockCompatible

@Module
final class Env(
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
    userTrustApi: lila.core.security.UserTrustApi,
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

  lazy val seekApi = wire[SeekApi]

  lazy val boardApiHookStream = wire[BoardApiHookStream]

  private lazy val lobbySyncActor = LobbySyncActor.start(
    broomPeriod = 2.seconds,
    resyncIdsPeriod = 25.seconds
  ): () =>
    wire[LobbySyncActor]

  // eager initialization for Bus subscription
  wire[AbortListener]

  private lazy val biter = wire[Biter]

  val socket = wire[LobbySocket]
