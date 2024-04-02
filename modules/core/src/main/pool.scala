package lila.core
package pool

import alleycats.Zero
import chess.Clock

import lila.core.rating.RatingRange
import lila.core.socket.Sri
import chess.ByColor

opaque type Blocking = Set[UserId]
object Blocking extends TotalWrapper[Blocking, Set[UserId]]:
  given Zero[Blocking] = Zero(Set.empty)

opaque type PoolConfigId = String
object PoolConfigId extends OpaqueString[PoolConfigId]

opaque type IsClockCompatible = Clock.Config => Boolean
object IsClockCompatible extends FunctionWrapper[IsClockCompatible, Clock.Config => Boolean]

case class PoolMember(
    userId: UserId,
    sri: lila.core.socket.Sri,
    rating: IntRating,
    ratingRange: Option[RatingRange],
    lame: Boolean,
    blocking: Blocking,
    rageSitCounter: Int,
    misses: Int = 0 // how many waves they missed
)

object PoolMember:
  given UserIdOf[PoolMember] = _.userId

case class Pairing(players: ByColor[(Sri, GameFullId)])
case class Pairings(pairings: List[Pairing])

object HookThieve:

  case class GetCandidates(clock: chess.Clock.Config, promise: Promise[PoolHooks])
  case class StolenHookIds(ids: Vector[String])

  case class PoolHook(hookId: String, member: PoolMember):
    def is(m: PoolMember) = member.userId == m.userId

  case class PoolHooks(hooks: Vector[PoolHook])

case class Joiner(
    sri: Sri,
    rating: IntRating,
    ratingRange: Option[RatingRange],
    lame: Boolean,
    blocking: Blocking
)(using val me: lila.core.user.MyId)

object Joiner:
  given UserIdOf[Joiner] = _.me.userId

trait PoolApi:
  def setOnlineSris(ids: socket.Sris): Unit
  def poolPerfKeys: Map[PoolConfigId, rating.PerfKey]
  def join(poolId: PoolConfigId, joiner: Joiner): Unit
  def leave(poolId: PoolConfigId, user: UserId): Unit
