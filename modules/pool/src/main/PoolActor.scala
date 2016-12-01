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
      self, ScheduledWave)

  scheduleWave

  def receive = {

    case Join(joiner) if !members.exists(_.userId == joiner.userId) =>
      members = members :+ PoolMember(joiner, config)
      if (members.size >= config.wave.players.value) self ! FullWave
      monitor.join.count(idString)()

    case Leave(userId) => members.find(_.userId == userId) foreach { member =>
      members = members.filter(member !=)
      monitor.leave.count(idString)()
      monitor.leave.wait(idString)(member.waitMillis)
    }

    case ScheduledWave =>
      monitor.wave.scheduled(idString)()
      runWave

    case FullWave =>
      monitor.wave.full(idString)()
      runWave
  }

  def runWave = {
    nextWave.cancel()
    val pairings = lila.mon.measure(_.lobby.pool.matchMaking.duration(idString)) {
      MatchMaking(members)
    }
    val pairedMembers = pairings.flatMap(_.members)
    members = members.diff(pairedMembers).map(_.incMisses)
    gameStarter(config, pairings).mon(_.lobby.pool.gameStart.duration(idString))

    monitor.wave.paired(idString)(pairedMembers.size)
    monitor.wave.missed(idString)(members.size)
    pairedMembers.foreach { m =>
      monitor.wave.wait(idString)(m.waitMillis)
    }
    pairings.foreach { p =>
      monitor.wave.ratingDiff(idString)(p.ratingDiff)
    }
    scheduleWave
  }

  val idString = config.id.value

  val monitor = lila.mon.lobby.pool
}

private object PoolActor {

  case class Join(joiner: PoolApi.Joiner) extends AnyVal
  case class Leave(userId: User.ID) extends AnyVal

  case object ScheduledWave
  case object FullWave
}
