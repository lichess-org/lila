package lila.pool

import scala.concurrent.duration._
import scala.util.Random

import akka.actor._
import org.joda.time.DateTime

import lila.user.User

private final class PoolActor(
    config: PoolConfig,
    gameStarter: GameStarter) extends Actor {

  import PoolActor._

  var members = Vector[PoolMember]()

  var nextWave: Cancellable = _

  def scheduleWave =
    nextWave = context.system.scheduler.scheduleOnce(
      config.wave.every + Random.nextInt(1000).millis,
      self, Wave)

  scheduleWave

  def receive = {

    case Join(joiner) =>
      members = members.filter(_.userId != joiner.userId) :+ PoolMember(joiner, config)
      if (members.size >= config.wave.players.value) self ! Wave

    case Leave(userId) =>
      members = members.filter(_.userId != userId)

    case Wave =>
      nextWave.cancel()
      val pairings = MatchMaking(members)
      members = members diff pairings.flatMap(_.members)
      gameStarter(config, pairings)
      scheduleWave
  }
}

private object PoolActor {

  case class Join(joiner: PoolApi.Joiner) extends AnyVal
  case class Leave(userId: User.ID) extends AnyVal

  case object Wave

}
