package lila.pool

import scala.concurrent.duration._
import lila.common.ThreadLocalRandom

import akka.actor._
import akka.pattern.pipe

import lila.socket.Socket.Sris
import lila.user.User

final private class PoolActor(
    config: PoolConfig,
    hookThieve: HookThieve,
    gameStarter: GameStarter
) extends Actor {

  import PoolActor._

  var members = Vector.empty[PoolMember]

  private var lastPairedUserIds = Set.empty[User.ID]

  var nextWave: Cancellable = _

  implicit def ec = context.dispatcher

  def scheduleWave() =
    nextWave = context.system.scheduler.scheduleOnce(
      config.wave.every + ThreadLocalRandom.nextInt(1000).millis,
      self,
      ScheduledWave
    )

  scheduleWave()

  def receive = {

    case Join(joiner, _) if lastPairedUserIds(joiner.userId) =>
    // don't pair someone twice in a row, it's probably a client error

    case Join(joiner, rageSit) =>
      members.find(joiner.is) match {
        case None =>
          members = members :+ PoolMember(joiner, config, rageSit)
          if (members.sizeIs >= config.wave.players.value) self ! FullWave
        case Some(member) if member.ratingRange != joiner.ratingRange =>
          members = members.map {
            case m if m == member => m withRange joiner.ratingRange
            case m                => m
          }
        case _ => // no change
      }

    case Leave(userId) =>
      members.find(_.userId == userId) foreach { member =>
        members = members.filter(member !=)
      }

    case ScheduledWave =>
      monitor.scheduled(monId).increment()
      self ! RunWave

    case FullWave =>
      monitor.full(monId).increment()
      self ! RunWave

    case RunWave =>
      nextWave.cancel()
      hookThieve.candidates(config.clock) pipeTo self
      ()

    case HookThieve.PoolHooks(hooks) =>
      monitor.withRange(monId).record(members.count(_.hasRange))

      val candidates = members ++ hooks.map(_.member)

      val pairings = MatchMaking(candidates)

      val pairedMembers = pairings.flatMap(_.members)

      hookThieve.stolen(
        hooks.filter { h =>
          pairedMembers.exists(h.is)
        },
        monId
      )

      members = members.diff(pairedMembers).map(_.incMisses)

      if (pairings.nonEmpty) gameStarter(config, pairings)

      monitor.candidates(monId).record(candidates.size)
      monitor.paired(monId).record(pairedMembers.size)
      monitor.missed(monId).record(members.size)
      pairings.foreach { p =>
        monitor.ratingDiff(monId).record(p.ratingDiff)
      }

      lastPairedUserIds = pairedMembers.view.map(_.userId).toSet

      scheduleWave()

    case Sris(sris) =>
      members = members.filter { m =>
        sris contains m.sri
      }
  }

  val monitor = lila.mon.lobby.pool.wave
  val monId   = config.id.value.replace('+', '_')
}

private object PoolActor {

  case class Join(joiner: PoolApi.Joiner, rageSit: lila.playban.RageSit)
  case class Leave(userId: User.ID) extends AnyVal

  case object ScheduledWave
  case object FullWave
  case object RunWave
}
