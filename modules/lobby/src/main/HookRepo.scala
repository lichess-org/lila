package lila.lobby

import org.joda.time.DateTime

import lila.common.{ Heapsort, MultiKeyMap }
import lila.socket.Socket.Sri
import scala.collection.View

// NOT thread safe.
// control concurrency from LobbyTrouper
final private class HookRepo {

  type ID = String

  private var hooks = MultiKeyMap[ID, Sri, Hook](Set.empty[Hook])(_.id, _.sri)

  private val hardLimit = 200

  implicit private val creationOrdering = Ordering.by[Hook, Long](_.createdAt.getMillis)

  def size = hooks.size

  // O(n)
  def filter(f: Hook => Boolean): View[Hook] = hooks.values.view.filter(f)

  // O(n + nb * log(n))
  def truncateIfNeeded() =
    if (hooks.size >= hardLimit) {
      logger.warn(s"Found ${hooks.size} hooks, cleaning up!")
      hooks = hooks reset Heapsort.topN(hooks.values, hardLimit * 3 / 4, creationOrdering).toSet
      logger.warn(s"Kept ${hooks.size} hooks")
    }

  def ids = hooks.key1s

  def byId(id: ID) = hooks get1 id

  def byIds(ids: Set[ID]) = ids.flatten(hooks.get1)

  def bySri(sri: Sri) = hooks get2 sri

  // O(n)
  // invoked when a hook is added
  def bySid(sid: String) = hooks.values.find(_.sid has sid)

  // O(n)
  // invoked regularly when cleaning up socket sris
  def notInSris(sris: Set[Sri]): Iterable[Hook] = hooks.values.filterNot(h => sris(h.sri))

  def save(hook: Hook): Unit = {
    hooks = hooks updated hook
  }

  def remove(hook: Hook): Unit = {
    hooks = hooks removed hook
  }

  // returns removed hooks
  def cleanupOld: Set[Hook] = {
    val limit   = DateTime.now minusMinutes 15
    val removed = hooks.values.view.filter(_.createdAt isBefore limit).toSet
    hooks = hooks removed removed
    removed
  }

  // O(n)
  // invoked regularly when stealing hooks for pools
  def poolCandidates(clock: chess.Clock.Config): Vector[lila.pool.HookThieve.PoolHook] =
    hooks.values.withFilter(_ compatibleWithPool clock).map(_.toPool).toVector
}
