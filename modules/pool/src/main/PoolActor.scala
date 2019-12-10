package lila.pool

import scala.concurrent.duration._
import scala.util.Random

import akka.actor._
import akka.pattern.pipe

import lila.socket.Socket.Sris
import lila.user.User

private final class PoolActor(
    config: PoolConfig,
    hookThieve: HookThieve,
    gameStarter: GameStarter
) extends Actor {

  import PoolActor._

  var members = Vector[PoolMember]()

  var nextWave: Cancellable = _

  def scheduleWave() =
    nextWave = context.system.scheduler.scheduleOnce(
      config.wave.every + Random.nextInt(1000).millis,
      self, ScheduledWave
    )

  scheduleWave()

  def receive = {

    case Join(joiner, rageSit) =>
      members.find(joiner.is) match {
        case None =>
          members = members :+ PoolMember(joiner, config, rageSit)
          if (members.size >= config.wave.players.value) self ! FullWave
          monitor.join.count(monId).increment()
        case Some(member) if member.ratingRange != joiner.ratingRange =>
          members = members.map {
            case m if m == member => m withRange joiner.ratingRange
            case m => m
          }
        case _ => // no change
      }

    case Leave(userId) => members.find(_.userId == userId) foreach { member =>
      members = members.filter(member !=)
      monitor.leave.count(monId).increment()
      monitor.leave.wait(monId).record(member.waitMillis)
    }

    case ScheduledWave =>
      monitor.wave.scheduled(monId).increment()
      self ! RunWave

    case FullWave =>
      monitor.wave.full(monId).increment()
      self ! RunWave

    case RunWave =>
      nextWave.cancel()
      hookThieve.candidates(config.clock, monId) pipeTo self

    case HookThieve.PoolHooks(hooks) => {

      monitor.wave.withRange(monId).record(members.count(_.hasRange))

      val candidates = members ++ hooks.map(_.member)

      val pairings = lila.common.Chronometer.syncMon(_.lobby.pool.matchMaking.duration(monId)) {
        MatchMaking(candidates)
      }

      val pairedMembers = pairings.flatMap(_.members)

      hookThieve.stolen(hooks.filter { h =>
        pairedMembers.exists(h.is)
      }, monId)

      members = members.diff(pairedMembers).map(_.incMisses)

      if (pairings.nonEmpty)
        gameStarter(config, pairings).mon(_.lobby.pool.gameStart.duration(monId))

      monitor.wave.candidates(monId).record(candidates.size)
      monitor.wave.paired(monId).record(pairedMembers.size)
      monitor.wave.missed(monId).record(members.size)
      pairedMembers.foreach { m =>
        monitor.wave.wait(monId).record(m.waitMillis)
      }
      pairings.foreach { p =>
        monitor.wave.ratingDiff(monId).record(p.ratingDiff)
      }

      scheduleWave()
    }

    case Sris(sris) =>
      members = members.filter { m =>
        sris contains m.sri
      }
  }

  val monitor = lila.mon.lobby.pool
  val monId = config.id.value.replace('+', '_')
}

private object PoolActor {

  case class Join(joiner: PoolApi.Joiner, rageSit: lila.playban.RageSit)
  case class Leave(userId: User.ID) extends AnyVal

  case object ScheduledWave
  case object FullWave
  case object RunWave
}
