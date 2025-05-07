package lila.lobby

import scalalib.HeapSort

import scala.collection.View

import lila.core.pool.IsClockCompatible
import lila.core.socket.Sri

// NOT thread safe.
// control concurrency from LobbySyncActor
final private class HookRepo:

  type ID = String

  private var hooks = MultiKeyMap[ID, Sri, Hook](Set.empty[Hook])(_.id, _.sri)

  private val hardLimit = 200

  private val creationOrdering = Ordering.by[Hook, Long](_.createdAt.toMillis)

  def size = hooks.size

  // O(n)
  def filter(f: Hook => Boolean): View[Hook] = hooks.values.view.filter(f)

  // O(n + nb * log(n))
  def truncateIfNeeded() =
    if hooks.size >= hardLimit then
      logger.warn(s"Found ${hooks.size} hooks, cleaning up!")
      hooks = hooks.reset(HeapSort.topN(hooks.values, hardLimit * 3 / 4)(using creationOrdering).toSet)
      logger.warn(s"Kept ${hooks.size} hooks")

  def ids = hooks.key1s

  def byId(id: ID) = hooks.get1(id)

  def byIds(ids: Set[ID]) = ids.flatten(hooks.get1)

  def bySri(sri: Sri) = hooks.get2(sri)

  // O(n)
  // invoked when a hook is added
  def bySid(sid: String) = hooks.values.find(_.sid.has(sid))

  // O(n)
  // invoked regularly when cleaning up socket sris
  def notInSris(sris: Set[Sri]): Iterable[Hook] = hooks.values.filterNot(h => sris(h.sri))

  def save(hook: Hook): Unit =
    hooks = hooks.updated(hook)

  def remove(hook: Hook): Unit =
    hooks = hooks.removed(hook)

  def exists(hook: Hook): Boolean =
    hooks.contains(hook.id)

  // returns removed hooks
  def cleanupOld: Set[Hook] =
    val limit   = nowInstant.minusMinutes(15)
    val removed = hooks.values.view.filter(_.createdAt.isBefore(limit)).toSet
    hooks = hooks.removed(removed)
    removed

  // O(n)
  // invoked regularly when stealing hooks for pools
  def poolCandidates(clock: chess.Clock.Config)(using
      IsClockCompatible
  ): Vector[lila.core.pool.HookThieve.PoolHook] =
    hooks.values.withFilter(_.compatibleWithPool(clock)).flatMap(toPool).toVector

  private def toPool(h: Hook) = h.user.map: u =>
    lila.core.pool.HookThieve.PoolHook(
      hookId = h.id,
      member = lila.core.pool.PoolMember(
        userId = u.id,
        sri = h.sri,
        rating = h.rating | lila.rating.Glicko.default.intRating,
        provisional = h.provisional,
        ratingRange = h.manualRatingRange,
        lame = h.user.so(_.lame),
        blocking = h.user.so(_.blocking),
        rageSitCounter = 0
      )
    )
