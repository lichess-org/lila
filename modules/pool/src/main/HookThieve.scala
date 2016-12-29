package lila.pool

import akka.actor.ActorSelection
import akka.pattern.ask

private final class HookThieve(lobby: ActorSelection) {

  import HookThieve._

  def candidates(clock: chess.Clock.Config, monId: String): Fu[PoolHooks] = {
    import makeTimeout.short
    lobby ? GetCandidates(clock) mapTo manifest[PoolHooks] addEffect { res =>
      lila.mon.lobby.pool.thieve.candidates(monId)(res.hooks.size)
    }
  } recover {
    case _ =>
      lila.mon.lobby.pool.thieve.timeout(monId)()
      PoolHooks(Vector.empty)
  }

  def stolen(poolHooks: Vector[PoolHook], monId: String) = {
    lila.mon.lobby.pool.thieve.stolen(monId)(poolHooks.size)
    if (poolHooks.nonEmpty) lobby ! StolenHookIds(poolHooks.map(_.hookId))
  }
}

object HookThieve {

  case class GetCandidates(clock: chess.Clock.Config)
  case class StolenHookIds(ids: Vector[String])

  case class PoolHook(hookId: String, member: PoolMember) {

    def is(m: PoolMember) = member.userId == m.userId
  }

  case class PoolHooks(hooks: Vector[PoolHook])
}
