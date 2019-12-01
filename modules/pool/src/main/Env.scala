package lila.pool

import com.softwaremill.macwire._
import scala.concurrent.duration._

import lila.common.Bus
import lila.game.Game
import lila.hub.FutureSequencer

final class Env(
    userRepo: lila.user.UserRepo,
    gameRepo: lila.game.GameRepo,
    idGenerator: lila.game.IdGenerator,
    playbanApi: lila.playban.PlaybanApi
)(implicit system: akka.actor.ActorSystem) {

  private lazy val hookThieve = wire[HookThieve]

  private lazy val configs = PoolList.all

  private lazy val sequencer = new FutureSequencer(
    system = system,
    executionTimeout = 5.seconds.some,
    logger = logger
  )

  private val onStart = (gameId: Game.Id) => Bus.publish(gameId, "gameStartId")

  private lazy val gameStarter = wire[GameStarter]

  lazy val api = wire[PoolApi]
}
