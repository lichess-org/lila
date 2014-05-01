package lila.pref

import scala.concurrent.duration.Duration

import lila.db.api._
import lila.memo.AsyncCache
import lila.user.User
import tube.prefTube

final class PrefApi(cacheTtl: Duration) {

  private def fetchPref(id: String): Fu[Option[Pref]] = $find byId id
  private val cache = AsyncCache(fetchPref, timeToLive = cacheTtl)

  def getPref(id: String): Fu[Pref] = cache(id) map (_ | Pref.create(id))
  def getPref(user: User): Fu[Pref] = getPref(user.id)
  def getPref(user: Option[User]): Fu[Pref] = user.fold(fuccess(Pref.default))(getPref)

  def getPref[A](user: User, pref: Pref => A): Fu[A] = getPref(user) map pref
  def getPref[A](userId: String, pref: Pref => A): Fu[A] = getPref(userId) map pref

  def setPref(pref: Pref): Funit =
    $save(pref) >>- { cache remove pref.id }

  def setPref(user: User, change: Pref => Pref): Funit =
    getPref(user) map change flatMap setPref

  def setPref(userId: String, change: Pref => Pref): Funit =
    getPref(userId) map change flatMap setPref

  def setPrefString(user: User, name: String, value: String): Funit =
    getPref(user) map { _.set(name, value) } flatten
      s"Bad pref ${user.id} $name -> $value" flatMap setPref
}
