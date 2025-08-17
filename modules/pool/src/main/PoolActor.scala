package lila.pool

import akka.actor.*
import akka.pattern.pipe
import scalalib.ThreadLocalRandom

import lila.core.pool.{ HookThieve, PoolMember }
import lila.core.socket.Sris

final private class PoolActor(
    config: PoolConfig,
    hookThieve: HookThieve,
    gameStarter: GameStarter
) extends Actor:

  import PoolActor.*

  var members = Vector.empty[PoolMember]

  private var lastPairedUserIds = Set.empty[UserId]

  var nextWave: Cancellable = scala.compiletime.uninitialized

  given Executor = context.dispatcher

  def scheduleWave() =
    nextWave = context.system.scheduler.scheduleOnce(
      config.wave.every + ThreadLocalRandom.nextInt(1000).millis,
      self,
      ScheduledWave
    )

  scheduleWave()

  def receive =

    case Join(joiner) if lastPairedUserIds(joiner.userId) =>
    // don't pair someone twice in a row, it's probably a client error

    case Join(joiner) =>
      members.find(m => joiner.userId.is(m.userId)) match
        case None =>
          members = members :+ joiner
          if members.sizeIs >= config.wave.players.value then self ! FullWave
        case Some(existing) if existing.ratingRange != joiner.ratingRange =>
          members = members.map: m =>
            if m == existing then m.withRange(joiner.ratingRange) else m
        case _ => // no change
    case Leave(userId) =>
      members.find(_.userId == userId).foreach { member =>
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
      hookThieve.candidates(config.clock).pipeTo(self)
      ()

    case HookThieve.PoolHooks(hooks) =>
      monitor.withRange(monId).record(members.count(_.hasRange))

      val candidates = members ++ hooks.map(_.member)

      val pairings = MatchMaking(candidates)

      val pairedMembers = pairings.flatMap(_.members)

      hookThieve.stolen(
        hooks.filter: h =>
          pairedMembers.exists(h.is(_)),
        monId
      )

      members = members.diff(pairedMembers).map(_.incMisses)

      if pairings.nonEmpty then gameStarter(config, pairings)

      monitor.candidates(monId).record(candidates.size)
      monitor.paired(monId).record(pairedMembers.size)
      monitor.missed(monId).record(members.size)
      pairings.foreach: p =>
        monitor.ratingDiff(monId).record(p.ratingDiff.value)

      lastPairedUserIds = pairedMembers.view.map(_.userId).toSet

      scheduleWave()

    case Sris(sris) =>
      members = members.filter(_.sri.forall(sris.contains))

  val monitor = lila.mon.lobby.pool.wave
  val monId = config.id.value.replace('+', '_')

private object PoolActor:

  case class Join(member: PoolMember) extends AnyVal
  case class Leave(userId: UserId) extends AnyVal

  case object ScheduledWave
  case object FullWave
  case object RunWave
