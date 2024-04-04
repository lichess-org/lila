package lila.lobby

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.core.config.*
import lila.core.pool.IsClockCompatible

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    onStart: lila.core.game.OnStart,
    relationApi: lila.relation.RelationApi,
    hasCurrentPlayban: lila.core.playban.HasCurrentPlayban,
    gameCache: lila.game.Cached,
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    userApi: lila.user.UserApi,
    gameRepo: lila.game.GameRepo,
    poolApi: lila.core.pool.PoolApi,
    cacheApi: lila.memo.CacheApi,
    socketKit: lila.core.socket.SocketKit
)(using Executor, akka.actor.ActorSystem, Scheduler, lila.game.IdGenerator, IsClockCompatible):

  private lazy val seekApiConfig = new SeekApi.Config(
    coll = db(CollName("seek")),
    archiveColl = db(CollName("seek_archive")),
    maxPerPage = MaxPerPage(13),
    maxPerUser = Max(5)
  )

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
    case lila.game.actorApi.AbortedBy(pov) => abortListener(pov)
