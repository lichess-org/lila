package lila.simul

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.Bus
import lila.common.config._
import lila.game.{ Game, GameRepo }
import lila.hub.{ Duct, DuctMap }
import lila.socket.Socket.{ GetVersion, SocketVersion }

@Module
private class RoundConfig(
    @ConfigName("collection.simul") val simulColl: CollName,
    @ConfigName("created.cache.ttl") val createdCacheTtl: FiniteDuration,
    @ConfigName("feature.views") val featureViews: Max
)

final class Env(
    appConfig: Configuration,
    db: lila.db.Env,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    renderer: lila.hub.actors.Renderer,
    timeline: lila.hub.actors.Timeline,
    chatApi: lila.chat.ChatApi,
    lightUser: lila.common.LightUser.Getter,
    onGameStart: lila.round.OnStart,
    isOnline: lila.user.IsOnline,
    asyncCache: lila.memo.AsyncCache.Builder,
    remoteSocketApi: lila.socket.RemoteSocket,
    proxyRepo: lila.round.GameProxyRepo
)(implicit system: ActorSystem) {

  private val config = appConfig.get[RoundConfig]("round")(AutoConfig.loader)

  private lazy val simulColl = db(config.simulColl)

  lazy val repo: SimulRepo = wire[SimulRepo]

  lazy val api: SimulApi = wire[SimulApi]

  lazy val jsonView = wire[JsonView]

  private val simulSocket = wire[SimulSocket]

  val isHosting = new lila.round.IsSimulHost(u => api.currentHostIds dmap (_ contains u))

  val allCreated = asyncCache.single(
    name = "simul.allCreated",
    repo.allCreated,
    expireAfter = _.ExpireAfterWrite(config.createdCacheTtl)
  )

  val allCreatedFeaturable = asyncCache.single(
    name = "simul.allCreatedFeaturable",
    repo.allCreatedFeaturable,
    expireAfter = _.ExpireAfterWrite(config.createdCacheTtl)
  )

  def featurable(simul: Simul): Boolean = featureLimiter(simul.hostId)(true)

  private val featureLimiter = new lila.memo.RateLimit[lila.user.User.ID](
    credits = config.featureViews.value,
    duration = 24 hours,
    name = "simul homepage views",
    key = "simul.feature",
    log = false
  )

  def version(simulId: Simul.ID) =
    simulSocket.rooms.ask[SocketVersion](simulId)(GetVersion)

  private val sequencerMap = new DuctMap(
    mkDuct = _ => Duct.extra.lazyFu,
    accessTimeout = 10.seconds
  )

  lazy val cleaner = new SimulCleaner(repo, api)

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
      case lila.hub.actorApi.round.SimulMoveEvent(move, simulId, opponentUserId) =>
        Bus.publish(
          lila.hub.actorApi.socket.SendTo(
            opponentUserId,
            lila.socket.Socket.makeMessage("simulPlayerMove", move.gameId)
          ),
          "socketUsers"
        )
    }
  )

  system.scheduler.scheduleWithFixedDelay(30 seconds, 30 seconds)(() => cleaner.cleanUp)
}
