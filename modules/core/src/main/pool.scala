package lila.core
package pool

import _root_.chess.{ Clock, ByColor }
import _root_.chess.IntRating
import alleycats.Zero

import scalalib.bus.NotBuseable

import lila.core.perf.PerfKey
import lila.core.rating.RatingRange
import lila.core.socket.Sri
import lila.core.userId.*
import lila.core.id.GameFullId

opaque type Blocking = Set[UserId]
object Blocking extends TotalWrapper[Blocking, Set[UserId]]:
  given Zero[Blocking] = Zero(Set.empty)

opaque type PoolConfigId = String
object PoolConfigId extends OpaqueString[PoolConfigId]

opaque type IsClockCompatible = Clock.Config => Boolean
object IsClockCompatible extends FunctionWrapper[IsClockCompatible, Clock.Config => Boolean]

enum PoolFrom:
  case Socket, Api, Hook

case class PoolMember(
    userId: UserId,
    sri: Sri,
    from: PoolFrom,
    rating: IntRating,
    provisional: Boolean,
    ratingRange: Option[RatingRange],
    lame: Boolean,
    blocking: Blocking,
    rageSitCounter: Int = 0,
    misses: Int = 0 // how many waves they missed
)

case class Pairing(players: ByColor[(Sri, GameFullId)])
case class Pairings(pairings: List[Pairing])

object HookThieve:

  enum HookBus:
    case GetCandidates(clock: Clock.Config, promise: Promise[PoolHooks])
    case StolenHookIds(ids: Vector[String])

  case class PoolHook(hookId: String, member: PoolMember) extends NotBuseable

  case class PoolHooks(hooks: Vector[PoolHook]) extends NotBuseable

trait PoolApi:
  def setOnlineSris(ids: socket.Sris): Unit
  def poolPerfKeys: Map[PoolConfigId, PerfKey]
  def join(poolId: PoolConfigId, member: PoolMember): Unit
  def leave(poolId: PoolConfigId, user: UserId): Unit
  def poolOf(clock: Clock.Config): Option[PoolConfigId]
