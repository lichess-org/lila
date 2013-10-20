package lila.pref

import lila.db.api._
import lila.user.User
import tube.prefTube

final class PrefApi {

  def getPref(id: String): Fu[Pref] = $find byId id map (_ | Pref.create(id))
  def getPref(user: User): Fu[Pref] = getPref(user.id)

  def getPref[A](user: User, pref: Pref ⇒ A): Fu[A] = getPref(user) map pref

  def setPref(pref: Pref): Funit = $save(pref)

  def setPref(user: User, change: Pref ⇒ Pref): Funit =
    getPref(user) map change flatMap setPref

  def setPrefString(user: User, name: String, value: String): Funit =
    getPref(user) map { _.set(name, value) } flatten
      s"Bad pref ${user.id} $name -> $value" flatMap setPref
}
