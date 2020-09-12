package lila.lobby

import org.joda.time.DateTime

import lila.common.Heapsort
import lila.socket.Socket.Sri

private object HookRepo {

  private var hooks = Vector[Hook]()

  private val hardLimit = 200

  implicit private val creationOrdering = Ordering.by[Hook, Long](_.createdAt.getMillis)

  def size = hooks.size

  def findCompatible(hook: Hook): Vector[Hook] = hooks.filter(_ compatibleWith hook)

  def truncateIfNeeded() =
    if (size >= hardLimit) {
      logger.warn(s"Found $size hooks, cleaning up!")
      hooks = Heapsort.topN(hooks, hardLimit * 2 / 3, creationOrdering)
      logger.warn(s"Kept ${hooks.size} hooks")
    }

  def vector = hooks

  def byId(id: String) = hooks find (_.id == id)

  def byIds(ids: Set[String]) =
    hooks filter { h =>
      ids contains h.id
    }

  def bySri(sri: Sri) = hooks find (_.sri == sri)

  def bySid(sid: String) = hooks find (_.sid == sid.some)

  def notInSris(sris: Set[Sri]): Vector[Hook] = hooks.filterNot(h => sris(h.sri))

  def save(hook: Hook): Unit = {
    hooks = hooks.filterNot(_.id == hook.id) :+ hook
  }

  def remove(hook: Hook): Unit = {
    hooks = hooks.filterNot(_.id == hook.id)
  }

  // returns removed hooks
  def cleanupOld = {
    val limit = DateTime.now minusMinutes 15
    partition(_.createdAt isAfter limit)
  }

  def poolCandidates(clock: chess.Clock.Config): Vector[lila.pool.HookThieve.PoolHook] =
    hooks.withFilter(_ compatibleWithPool clock).map(_.toPool)

  // keeps hooks that hold true
  // returns removed hooks
  private def partition(f: Hook => Boolean): Vector[Hook] = {
    val (kept, removed) = hooks partition f
    hooks = kept
    removed
  }
}
