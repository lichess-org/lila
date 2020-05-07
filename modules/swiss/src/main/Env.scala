package lila.swiss

import com.softwaremill.macwire._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._
import lila.common.{ AtMost, Every, ResilientScheduler }
import lila.socket.Socket.{ GetVersion, SocketVersion }

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    onStart: lila.round.OnStart,
    remoteSocketApi: lila.socket.RemoteSocket,
    chatApi: lila.chat.ChatApi,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi,
    roundSocket: lila.round.RoundSocket
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    // mat: akka.stream.Materializer,
    idGenerator: lila.game.IdGenerator,
    mode: play.api.Mode
) {

  private val colls = wire[SwissColls]

  private val pairingSystem = new PairingSystem(appConfig.get[String]("swiss.bbpairing"))

  private val scoring = wire[SwissScoring]

  private val director = wire[SwissDirector]

  val api = wire[SwissApi]

  private lazy val socket = wire[SwissSocket]

  def version(swissId: Swiss.Id): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](swissId.value)(GetVersion)

  lazy val standingApi = wire[SwissStandingApi]

  private lazy val rankingApi = wire[SwissRankingApi]

  lazy val json = wire[SwissJson]

  lazy val forms = wire[SwissForm]

  private lazy val cache: SwissCache = wire[SwissCache]

  lazy val getName = new GetSwissName(cache.name.sync)

  lila.common.Bus.subscribeFun(
    "finishGame",
    "adjustCheater",
    "adjustBooster"
  ) {
    case lila.game.actorApi.FinishGame(game, _, _) => api finishGame game
    // case lila.hub.actorApi.mod.MarkCheater(userId, true) => api.ejectLame(userId, _)
    // case lila.hub.actorApi.mod.MarkBooster(userId)       => api.ejectLame(userId, Nil)
  }

  ResilientScheduler(
    every = Every(2 seconds),
    atMost = AtMost(15 seconds),
    initialDelay = 20 seconds
  ) { api.startPendingRounds }

  ResilientScheduler(
    every = Every(20 seconds),
    atMost = AtMost(15 seconds),
    // initialDelay = 20 seconds
    initialDelay = 10 seconds
  ) { api.checkOngoingGames }
}

private class SwissColls(db: lila.db.Db) {
  val swiss   = db(CollName("swiss"))
  val player  = db(CollName("swiss_player"))
  val pairing = db(CollName("swiss_pairing"))
}
