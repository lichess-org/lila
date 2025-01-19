package lila.simul

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.Bus
import lila.common.autoconfig.{ *, given }
import lila.core.config.*
import lila.core.socket.{ GetVersion, SocketVersion }
import lila.core.user.FlairGet

@Module
private class SimulConfig(
    @ConfigName("collection.simul") val simulColl: CollName,
    @ConfigName("feature.views") val featureViews: Max
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    gameRepo: lila.core.game.GameRepo,
    newPlayer: lila.core.game.NewPlayer,
    userApi: lila.core.user.UserApi,
    chat: lila.core.chat.ChatApi,
    lightUser: lila.core.LightUser.GetterFallback,
    onGameStart: lila.core.game.OnStart,
    cacheApi: lila.memo.CacheApi,
    historyApi: lila.core.history.HistoryApi,
    socketKit: lila.core.socket.SocketKit,
    socketReq: lila.core.socket.SocketRequester,
    gameProxy: lila.core.game.GameProxy,
    isOnline: lila.core.socket.IsOnline
)(using Executor, Scheduler, play.api.Mode, FlairGet, lila.core.config.RateLimit):

  private val config = appConfig.get[SimulConfig]("simul")(AutoConfig.loader)

  private lazy val simulColl = db(config.simulColl)

  lazy val repo: SimulRepo = wire[SimulRepo]

  lazy val verify = wire[SimulCondition.Verify]

  lazy val api: SimulApi = wire[SimulApi]

  lazy val jsonView = wire[JsonView]

  private val simulSocket = wire[SimulSocket]

  val allCreatedFeaturable = cacheApi.unit[List[Simul]]:
    _.refreshAfterWrite(3.seconds).buildAsyncFuture(_ => repo.allCreatedFeaturable)

  val featurable = SimulIsFeaturable: simul =>
    simul.conditions.teamMember.isEmpty && featureLimiter.zero(simul.hostId)(true)

  private val featureLimiter = lila.memo.RateLimit[UserId](
    credits = config.featureViews.value,
    duration = 24.hours,
    key = "simul.feature",
    log = false
  )

  def version(simulId: SimulId) =
    simulSocket.rooms.ask[SocketVersion](simulId.into(RoomId))(GetVersion.apply)

  Bus.subscribeFuns(
    "finishGame" -> { case lila.core.game.FinishGame(game, _) =>
      api.finishGame(game)
      ()
    },
    "adjustCheater" -> { case lila.core.mod.MarkCheater(userId, true) =>
      api.ejectCheater(userId)
      ()
    },
    "moveEventSimul" -> { case lila.core.round.SimulMoveEvent(move, _, opponentUserId) =>
      import lila.common.Json.given
      Bus.publish(
        lila.core.socket.SendTo(
          opponentUserId,
          lila.core.socket.makeMessage("simulPlayerMove", move.gameId)
        ),
        "socketUsers"
      )
    }
  )

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    repo.anonymizeHost(del.id)
    repo.anonymizePlayers(del.id)

final class SimulIsFeaturable(f: Simul => Boolean) extends (Simul => Boolean):
  def apply(simul: Simul) = f(simul)
