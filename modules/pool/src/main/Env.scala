package lila.pool

import com.softwaremill.macwire.*

import lila.common.Bus
import lila.game.Game

@Module
final class Env(
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    gameRepo: lila.game.GameRepo,
    idGenerator: lila.game.IdGenerator,
    HasCurrentPlayban: lila.core.playban.HasCurrentPlayban,
    rageSitOf: lila.core.playban.RageSitOf
)(using Executor, akka.actor.ActorSystem, Scheduler):

  private val hookThieve = wire[HookThieve]

  val onStart = (gameId: GameId) => Bus.publish(Game.OnStart(gameId), "gameStartId")

  private val gameStarter = wire[GameStarter]

  val api = wire[PoolApi]

  export PoolList.{ all, isClockCompatible }
