package lila.pool

import scala.concurrent.Promise

import lila.common.Bus

private final class HookThieve()(implicit system: akka.actor.ActorSystem) {

  import HookThieve._

  def candidates(clock: chess.Clock.Config, monId: String): Fu[PoolHooks] =
    Bus.ask[PoolHooks]('lobbyTrouper)(GetCandidates(clock, _)) recover {
      case _ =>
        lila.mon.lobby.pool.thieve.timeout(monId)()
        PoolHooks(Vector.empty)
    }

  def stolen(poolHooks: Vector[PoolHook], monId: String) = {
    lila.mon.lobby.pool.thieve.stolen(monId)(poolHooks.size)
    if (poolHooks.nonEmpty) Bus.publish(StolenHookIds(poolHooks.map(_.hookId)), 'lobbyTrouper)
  }
}

object HookThieve {

  case class GetCandidates(clock: chess.Clock.Config, promise: Promise[PoolHooks])
  case class StolenHookIds(ids: Vector[String])

  case class PoolHook(hookId: String, member: PoolMember) {
    def is(m: PoolMember) = member.userId == m.userId
  }

  case class PoolHooks(hooks: Vector[PoolHook])
}
