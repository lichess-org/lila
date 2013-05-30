package lila.lobby

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

object HookRepo {

  private var hooks = Vector[Hook]()

  def list = hooks.toList

  def byId(id: String) = hooks find (_.id == id)

  def byUid(uid: String) = hooks find (_.uid == uid)

  def allOpen: List[Hook] = list.filter(_.open)

  def allOpenCasual = list.filter(h ⇒ h.open && h.mode == 0)

  def openNotInUids(uids: Set[String]): List[Hook] = allOpen.filterNot(h ⇒ uids(h.uid))

  def save(hook: Hook) {
    hooks = hooks.filterNot(_.id == hook.id) :+ hook
  }

  def remove(hook: Hook) {
    hooks = hooks filterNot (_.id == hook.id)
  }

  // returns removed hooks
  def removeUid(uid: String) = partition(_.uid != uid)

  // returns removed hooks
  def cleanupOld = {
    val limit = DateTime.now - 10.minutes
    partition(_.createdAt > limit)
  }

  // keeps hooks that hold true
  // returns removed hooks
  private def partition(f: Hook ⇒ Boolean): List[Hook] = {
    val (kept, removed) = hooks partition f
    hooks = kept
    removed.toList
  }
}
