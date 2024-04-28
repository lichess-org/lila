package lila.pool

import com.softwaremill.macwire.*

@Module
final class Env(
    userApi: lila.core.user.UserApi,
    gameRepo: lila.core.game.GameRepo,
    newPlayer: lila.core.game.NewPlayer,
    idGenerator: lila.core.game.IdGenerator,
    HasCurrentPlayban: lila.core.playban.HasCurrentPlayban,
    rageSitOf: lila.core.playban.RageSitOf
)(using Executor, akka.actor.ActorSystem, Scheduler):

  private val hookThieve = wire[HookThieve]

  val onStart = (gameId: GameId) => lila.common.Bus.publish(lila.core.game.GameStart(gameId), "gameStartId")

  private val gameStarter = wire[GameStarter]

  val api = wire[PoolApi]

  export PoolList.{ all, isClockCompatible }
