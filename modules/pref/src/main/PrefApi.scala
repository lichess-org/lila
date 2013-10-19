package lila.pref

import lila.user.User
import tube.prefTube
import lila.db.api._

final class PrefApi {

  def getPref(id: String): Fu[Pref] = $find byId id map (_ | Pref.default)
  def getPref(user: User): Fu[Pref] = getPref(user.id)

  def getPref[A](user: User, pref: Pref ⇒ A): Fu[A] = getPref(user) map pref

  def getPrefString(user: User, name: String): Fu[Option[String]] = 
    getPref(user) map (_ get name)

  def setPref(pref: Pref): Funit = $update(pref)

  def setPref(user: User, change: Pref ⇒ Pref): Funit = 
    getPref(user) map change flatMap setPref

  def setPrefString(user: User, name: String, value: String): Funit = 
    getPref(user) map (_ get name)
}
