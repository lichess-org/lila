package lila.pref

import play.api.libs.json.Json
import scala.concurrent.duration.Duration

import lila.db.BSON
import lila.db.Types._
import lila.memo.AsyncCache
import lila.user.User
import reactivemongo.bson._

final class PrefApi(coll: Coll, cacheTtl: Duration) {

  private def fetchPref(id: String): Fu[Option[Pref]] = coll.find(BSONDocument("_id" -> id)).one[Pref]
  private val cache = AsyncCache(fetchPref, timeToLive = cacheTtl)

  private implicit val prefBSONHandler = new BSON[Pref] {

    import lila.db.BSON.MapValue.{ MapReader, MapWriter }
    implicit val tagsReader = MapReader[String]
    implicit val tagsWriter = MapWriter[String]

    def reads(r: BSON.Reader): Pref = Pref(
      _id = r str "_id",
      dark = r.getD("dark", Pref.default.dark),
      is3d = r.getD("is3d", Pref.default.is3d),
      theme = r.getD("theme", Pref.default.theme),
      pieceSet = r.getD("pieceSet", Pref.default.pieceSet),
      theme3d = r.getD("theme3d", Pref.default.theme3d),
      pieceSet3d = r.getD("pieceSet3d", Pref.default.pieceSet3d),
      autoQueen = r.getD("autoQueen", Pref.default.autoQueen),
      autoThreefold = r.getD("autoThreefold", Pref.default.autoThreefold),
      takeback = r.getD("takeback", Pref.default.takeback),
      clockTenths = r.getD("clockTenths", Pref.default.clockTenths),
      clockBar = r.getD("clockBar", Pref.default.clockBar),
      clockSound = r.getD("clockSound", Pref.default.clockSound),
      premove = r.getD("premove", Pref.default.premove),
      animation = r.getD("animation", Pref.default.animation),
      captured = r.getD("captured", Pref.default.captured),
      follow = r.getD("follow", Pref.default.follow),
      highlight = r.getD("highlight", Pref.default.highlight),
      destination = r.getD("destination", Pref.default.destination),
      coords = r.getD("coords", Pref.default.coords),
      replay = r.getD("replay", Pref.default.replay),
      challenge = r.getD("challenge", Pref.default.challenge),
      coordColor = r.getD("coordColor", Pref.default.coordColor),
      puzzleDifficulty = r.getD("puzzleDifficulty", Pref.default.puzzleDifficulty),
      tags = r.getD("tags", Pref.default.tags))

    def writes(w: BSON.Writer, o: Pref) = BSONDocument(
      "_id" -> o._id,
      "dark" -> o.dark,
      "is3d" -> o.is3d,
      "theme" -> o.theme,
      "pieceSet" -> o.pieceSet,
      "theme3d" -> o.theme3d,
      "pieceSet3d" -> o.pieceSet3d,
      "autoQueen" -> o.autoQueen,
      "autoThreefold" -> o.autoThreefold,
      "takeback" -> o.takeback,
      "clockTenths" -> o.clockTenths,
      "clockBar" -> o.clockBar,
      "clockSound" -> o.clockSound,
      "premove" -> o.premove,
      "animation" -> o.animation,
      "captured" -> o.captured,
      "follow" -> o.follow,
      "highlight" -> o.highlight,
      "destination" -> o.destination,
      "coords" -> o.coords,
      "replay" -> o.replay,
      "challenge" -> o.challenge,
      "coordColor" -> o.coordColor,
      "puzzleDifficulty" -> o.puzzleDifficulty,
      "tags" -> o.tags)
  }

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
      _ flatMap (_.getAs[Boolean]("follow")) getOrElse Pref.default.follow
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
