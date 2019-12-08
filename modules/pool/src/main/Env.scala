package lila.pool

import com.softwaremill.macwire._
import scala.concurrent.duration._

import lila.common.Bus
import lila.game.Game
import lila.hub.FutureSequencer

@Module
final class Env(
    userRepo: lila.user.UserRepo,
    gameRepo: lila.game.GameRepo,
    idGenerator: lila.game.IdGenerator,
    playbanApi: lila.playban.PlaybanApi
)(implicit system: akka.actor.ActorSystem) {

  private lazy val hookThieve = wire[HookThieve]

  private lazy val sequencer = new FutureSequencer(
    executionTimeout = 5.seconds.some
  )

  private val onStart = (gameId: Game.Id) => Bus.publish(gameId, "gameStartId")

  private lazy val gameStarter = wire[GameStarter]

  lazy val api = wire[PoolApi]

  def poolConfigs = PoolList.all
}
