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

  def removeUid(uid: String) {
    hooks = hooks filterNot (_.uid == uid)
  }

  def cleanupOld {
    val limit = DateTime.now - 20.minutes
    hooks = hooks filter (_.createdAt > limit)
  }
}
