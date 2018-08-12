package lila.lobby

import org.joda.time.DateTime

import lila.socket.Socket.Uid

object HookRepo {

  private var hooks = Vector[Hook]()

  private val hardLimit = 150

  def size = hooks.size

  def findCompatible(hook: Hook): Vector[Hook] = hooks filter (_ compatibleWith hook)

  def truncateIfNeeded = if (size >= hardLimit) {
    logger.warn(s"Found ${size} hooks, cleaning up!")
    hooks = hooks.sortBy(-_.createdAt.getMillis).take(hardLimit * 2 / 3)
    logger.warn(s"Kept ${hooks.size} hooks")
  }

  def vector = hooks

  def byId(id: String) = hooks find (_.id == id)

  def byIds(ids: Set[String]) = hooks filter { h => ids contains h.id }

  def byUid(uid: Uid) = hooks find (_.uid == uid)

  def bySid(sid: String) = hooks find (_.sid == sid.some)

  def notInUids(uids: Set[Uid]): Vector[Hook] = hooks.filterNot(h => uids(h.uid))

  def save(hook: Hook): Unit = {
    hooks = hooks.filterNot(_.id == hook.id) :+ hook
  }

  def remove(hook: Hook): Unit = {
    hooks = hooks.filterNot(_.id == hook.id)
  }

  // returns removed hooks
  def cleanupOld = {
    val limit = DateTime.now minusMinutes 10
    partition(_.createdAt isAfter limit)
  }

  def poolCandidates(clock: chess.Clock.Config): Vector[lila.pool.HookThieve.PoolHook] =
    hooks.filter(_ compatibleWithPool clock).map(_.toPool)

  // keeps hooks that hold true
  // returns removed hooks
  private def partition(f: Hook => Boolean): Vector[Hook] = {
    val (kept, removed) = hooks partition f
    hooks = kept
    removed
  }
}
