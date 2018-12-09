package lila.pool

import scala.concurrent.Promise

private final class HookThieve(bus: lila.common.Bus) {

  import HookThieve._

  def candidates(clock: chess.Clock.Config, monId: String): Fu[PoolHooks] =
    bus.ask[PoolHooks]('lobbyTrouper)(GetCandidates(clock, _)) recover {
      case _ =>
        lila.mon.lobby.pool.thieve.timeout(monId)()
        PoolHooks(Vector.empty)
    }

  def stolen(poolHooks: Vector[PoolHook], monId: String) = {
    lila.mon.lobby.pool.thieve.stolen(monId)(poolHooks.size)
    if (poolHooks.nonEmpty) bus.publish(StolenHookIds(poolHooks.map(_.hookId)), 'lobbyTrouper)
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
