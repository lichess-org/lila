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
    playbanApi: lila.playban.PlaybanApi
)(using Executor, akka.actor.ActorSystem, Scheduler):

  private lazy val hookThieve = wire[HookThieve]

  private val onStart = (gameId: GameId) => Bus.publish(Game.OnStart(gameId), "gameStartId")

  private lazy val gameStarter = wire[GameStarter]

  lazy val api = wire[PoolApi]

  def poolConfigs = PoolList.all
