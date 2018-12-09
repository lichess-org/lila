package lidraughts.pool

import scala.concurrent.Promise

private final class HookThieve(bus: lidraughts.common.Bus) {

  import HookThieve._

  def candidates(clock: draughts.Clock.Config, monId: String): Fu[PoolHooks] =
    bus.ask[PoolHooks]('lobby)(GetCandidates(clock, _)) recover {
      case _ =>
        lidraughts.mon.lobby.pool.thieve.timeout(monId)()
        PoolHooks(Vector.empty)
    }

  def stolen(poolHooks: Vector[PoolHook], monId: String) = {
    lidraughts.mon.lobby.pool.thieve.stolen(monId)(poolHooks.size)
    if (poolHooks.nonEmpty) bus.publish(StolenHookIds(poolHooks.map(_.hookId)), 'lobby)
  }
}

object HookThieve {

  case class GetCandidates(clock: draughts.Clock.Config, promise: Promise[PoolHooks])
  case class StolenHookIds(ids: Vector[String])

  case class PoolHook(hookId: String, member: PoolMember) {
    def is(m: PoolMember) = member.userId == m.userId
  }

  case class PoolHooks(hooks: Vector[PoolHook])
}
