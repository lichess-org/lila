package lila.pref

import play.api.libs.json.Json
import scala.concurrent.duration.Duration

import lila.db.Types._
import lila.memo.AsyncCache
import lila.user.User
import reactivemongo.bson._

final class PrefApi(coll: Coll, cacheTtl: Duration) {

  private def fetchPref(id: String): Fu[Option[Pref]] = coll.find(BSONDocument("_id" -> id)).one[Pref]
  private val cache = AsyncCache(fetchPref, timeToLive = cacheTtl)

  import lila.db.BSON.MapValue._
  implicit val reader = MapReader[String]
  implicit val writer = MapWriter[String]
  private implicit val prefBSONHandler = Macros.handler[Pref]

  def saveTag(user: User, name: String, value: String) =
    coll.update(
      BSONDocument("_id" -> user.id),
      BSONDocument("$set" -> BSONDocument(s"tags.$name" -> value)),
      upsert = true).void >>- { cache remove user.id }

  def getPref(id: String): Fu[Pref] = cache(id) map (_ getOrElse Pref.create(id))
  def getPref(user: User): Fu[Pref] = getPref(user.id)
  def getPref(user: Option[User]): Fu[Pref] = user.fold(fuccess(Pref.default))(getPref)

  def getPref[A](user: User, pref: Pref => A): Fu[A] = getPref(user) map pref
  def getPref[A](userId: String, pref: Pref => A): Fu[A] = getPref(userId) map pref

  def followable(userId: String): Fu[Boolean] =
    coll.find(BSONDocument("_id" -> userId), BSONDocument("follow" -> true)).one[BSONDocument] map {
      _ flatMap (_.getAs[Boolean]("follow")) getOrElse false
    }

  def unfollowableIds(userIds: List[String]): Fu[Set[String]] =
    coll.find(BSONDocument(
      "_id" -> BSONDocument("$in" -> userIds),
      "follow" -> false
    ), BSONDocument("_id" -> true)).cursor[BSONDocument].collect[List]() map {
      _.flatMap(_.getAs[String]("_id")).toSet
    }

  def followableIds(userIds: List[String]): Fu[Set[String]] =
    unfollowableIds(userIds) map (uns => userIds.toSet diff uns)

  def followables(userIds: List[String]): Fu[List[Boolean]] =
    followableIds(userIds) map { followables => userIds map followables.contains }

  def setPref(pref: Pref): Funit =
    coll.update(BSONDocument("_id" -> pref.id), pref, upsert = true).void >>- { cache remove pref.id }

  def setPref(user: User, change: Pref => Pref): Funit =
    getPref(user) map change flatMap setPref

  def setPref(userId: String, change: Pref => Pref): Funit =
    getPref(userId) map change flatMap setPref

  def setPrefString(user: User, name: String, value: String): Funit =
    getPref(user) map { _.set(name, value) } flatten
      s"Bad pref ${user.id} $name -> $value" flatMap setPref
}
