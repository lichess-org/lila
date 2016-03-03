package lila.lobby

import org.joda.time.DateTime

object HookRepo {

  private var hooks = Vector[Hook]()

  private val hardLimit = 100

  def findCompatible(hook: Hook): List[Hook] = list filter (_ compatibleWith hook)

  def truncateIfNeeded {
    if (hooks.size > hardLimit) {
      play.api.Logger("lobby").warn(s"Found ${hooks.size} hooks, cleaning up!")
      cleanupOld
      hooks = hooks.take(hardLimit / 2)
    }
  }

  def list = {
    truncateIfNeeded
    hooks.toList
  }

  def byId(id: String) = hooks find (_.id == id)

  def byUid(uid: String) = hooks find (_.uid == uid)

  def bySid(sid: String) = hooks find (_.sid == sid.some)

  def notInUids(uids: Set[String]): List[Hook] = list.filterNot(h => uids(h.uid))

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
  private def partition(f: Hook => Boolean): List[Hook] = {
    val (kept, removed) = hooks partition f
    hooks = kept
    removed.toList
  }
}
