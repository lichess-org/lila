package lila.lobby

import org.joda.time.DateTime

object HookRepo {

  private var hooks = Vector[Hook]()

  private val hardLimit = 150

  def size = hooks.size

  def findCompatible(hook: Hook): Vector[Hook] = hooks filter (_ compatibleWith hook)

  def truncateIfNeeded = if (size >= hardLimit) {
    play.api.Logger("lobby").warn(s"Found ${size} hooks, cleaning up!")
    cleanupOld
    hooks = hooks.take(hardLimit / 2)
  }

  def vector = hooks

  def byId(id: String) = hooks find (_.id == id)

  def byUid(uid: String) = hooks find (_.uid == uid)

  def bySid(sid: String) = hooks find (_.sid == sid.some)

  def notInUids(uids: Set[String]): Vector[Hook] = hooks.filterNot(h => uids(h.uid))

  def save(hook: Hook) {
    hooks = hooks.filterNot(_.id == hook.id) :+ hook
  }

  def remove(hook: Hook) {
    hooks = hooks filterNot (_.id == hook.id)
  }

  // returns removed hooks
  def cleanupOld = {
    val limit = DateTime.now minusMinutes 10
    partition(_.createdAt isAfter limit)
  }

  // keeps hooks that hold true
  // returns removed hooks
  private def partition(f: Hook => Boolean): Vector[Hook] = {
    val (kept, removed) = hooks partition f
    hooks = kept
    removed
  }
}
