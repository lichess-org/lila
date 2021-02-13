package lila.simul

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.Bus
import lila.common.config._
import lila.socket.Socket.{ GetVersion, SocketVersion }

@Module
private class SimulConfig(
    @ConfigName("collection.simul") val simulColl: CollName,
    @ConfigName("feature.views") val featureViews: Max
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    renderer: lila.hub.actors.Renderer,
    timeline: lila.hub.actors.Timeline,
    chatApi: lila.chat.ChatApi,
    lightUser: lila.common.LightUser.Getter,
    onGameStart: lila.round.OnStart,
    cacheApi: lila.memo.CacheApi,
    remoteSocketApi: lila.socket.RemoteSocket,
    proxyRepo: lila.round.GameProxyRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem,
    mode: play.api.Mode
) {

  private val config = appConfig.get[SimulConfig]("simul")(AutoConfig.loader)

  private lazy val simulColl = db(config.simulColl)

  lazy val repo: SimulRepo = wire[SimulRepo]

  lazy val api: SimulApi = wire[SimulApi]

  lazy val jsonView = wire[JsonView]

  private val simulSocket = wire[SimulSocket]

  val isHosting = new lila.round.IsSimulHost(u => api.currentHostIds dmap (_ contains u))

  val allCreatedFeaturable = cacheApi.unit[List[Simul]] {
    _.refreshAfterWrite(3 seconds)
      .buildAsyncFuture(_ => repo.allCreatedFeaturable)
  }

  val featurable = new SimulIsFeaturable((simul: Simul) =>
    simul.team.isEmpty && featureLimiter(simul.hostId)(true)(false)
  )

  private val featureLimiter = new lila.memo.RateLimit[lila.user.User.ID](
    credits = config.featureViews.value,
    duration = 24 hours,
    key = "simul.feature",
    log = false
  )

  def version(simulId: Simul.ID) =
    simulSocket.rooms.ask[SocketVersion](simulId)(GetVersion)

  Bus.subscribeFuns(
    "finishGame" -> {
      case lila.game.actorApi.FinishGame(game, _, _) => api finishGame game
    },
    "adjustCheater" -> {
      case lila.hub.actorApi.mod.MarkCheater(userId, true) => api ejectCheater userId
    },
    "simulGetHosts" -> {
      case lila.hub.actorApi.simul.GetHostIds(promise) => promise completeWith api.currentHostIds
    },
    "moveEventSimul" -> {
      case lila.hub.actorApi.round.SimulMoveEvent(move, _, opponentUserId) =>
        Bus.publish(
          lila.hub.actorApi.socket.SendTo(
            opponentUserId,
            lila.socket.Socket.makeMessage("simulPlayerMove", move.gameId)
          ),
          "socketUsers"
        )
    }
  )
}

final class SimulIsFeaturable(f: Simul => Boolean) extends (Simul => Boolean) {
  def apply(simul: Simul) = f(simul)
}
