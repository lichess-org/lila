package lila.pref

import play.api.libs.json.Json
import scala.concurrent.duration.Duration

import lila.db.BSON
import lila.db.dsl._
import lila.hub.actorApi.SendTo
import lila.memo.AsyncCache
import lila.user.User
import reactivemongo.bson._

final class PrefApi(
    coll: Coll,
    cacheTtl: Duration,
    bus: lila.common.Bus) {

  private def fetchPref(id: String): Fu[Option[Pref]] = coll.find(BSONDocument("_id" -> id)).uno[Pref]
  private val cache = AsyncCache(fetchPref, timeToLive = cacheTtl)

  private implicit val prefBSONHandler = new BSON[Pref] {

    import lila.db.BSON.MapValue.{ MapReader, MapWriter }
    implicit val tagsReader = MapReader[String]
    implicit val tagsWriter = MapWriter[String]

    def reads(r: BSON.Reader): Pref = Pref(
      _id = r str "_id",
      dark = r.getD("dark", Pref.default.dark),
      transp = r.getD("transp", Pref.default.transp),
      bgImg = r.strO("bgImg"),
      is3d = r.getD("is3d", Pref.default.is3d),
      theme = r.getD("theme", Pref.default.theme),
      pieceSet = r.getD("pieceSet", Pref.default.pieceSet),
      theme3d = r.getD("theme3d", Pref.default.theme3d),
      pieceSet3d = r.getD("pieceSet3d", Pref.default.pieceSet3d),
      soundSet = r.getD("soundSet", Pref.default.soundSet),
      blindfold = r.getD("blindfold", Pref.default.blindfold),
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
      message = r.getD("message", Pref.default.message),
      coordColor = r.getD("coordColor", Pref.default.coordColor),
      puzzleDifficulty = r.getD("puzzleDifficulty", Pref.default.puzzleDifficulty),
      submitMove = r.getD("submitMove", Pref.default.submitMove),
      confirmResign = r.getD("confirmResign", Pref.default.confirmResign),
      insightShare = r.getD("insightShare", Pref.default.insightShare),
      tags = r.getD("tags", Pref.default.tags))

    def writes(w: BSON.Writer, o: Pref) = BSONDocument(
      "_id" -> o._id,
      "dark" -> o.dark,
      "transp" -> o.transp,
      "bgImg" -> o.bgImg,
      "is3d" -> o.is3d,
      "theme" -> o.theme,
      "pieceSet" -> o.pieceSet,
      "theme3d" -> o.theme3d,
      "pieceSet3d" -> o.pieceSet3d,
      "soundSet" -> o.soundSet,
      "blindfold" -> o.blindfold,
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
      "message" -> o.message,
      "coordColor" -> o.coordColor,
      "puzzleDifficulty" -> o.puzzleDifficulty,
      "submitMove" -> o.submitMove,
      "confirmResign" -> o.confirmResign,
      "insightShare" -> o.insightShare,
      "tags" -> o.tags)
  }

  def saveTag(user: User, name: String, value: String) =
    coll.update(
      BSONDocument("_id" -> user.id),
      BSONDocument("$set" -> BSONDocument(s"tags.$name" -> value)),
      upsert = true).void >>- { cache remove user.id }

  def getPrefById(id: String): Fu[Pref] = cache(id) map (_ getOrElse Pref.create(id))
  val getPref = getPrefById _
  def getPref(user: User): Fu[Pref] = getPref(user.id)
  def getPref(user: Option[User]): Fu[Pref] = user.fold(fuccess(Pref.default))(getPref)

  def getPref[A](user: User, pref: Pref => A): Fu[A] = getPref(user) map pref
  def getPref[A](userId: String, pref: Pref => A): Fu[A] = getPref(userId) map pref

  def followable(userId: String): Fu[Boolean] =
    coll.find(BSONDocument("_id" -> userId), BSONDocument("follow" -> true)).uno[BSONDocument] map {
      _ flatMap (_.getAs[Boolean]("follow")) getOrElse Pref.default.follow
    }

  def unfollowableIds(userIds: List[String]): Fu[Set[String]] =
    coll.distinct("_id", BSONDocument(
      "_id" -> BSONDocument("$in" -> userIds),
      "follow" -> false
    ).some) map lila.db.BSON.asStringSet

  def followableIds(userIds: List[String]): Fu[Set[String]] =
    unfollowableIds(userIds) map userIds.toSet.diff

  def followables(userIds: List[String]): Fu[List[Boolean]] =
    followableIds(userIds) map { followables => userIds map followables.contains }

  def setPref(pref: Pref, notifyChange: Boolean): Funit =
    coll.update(BSONDocument("_id" -> pref.id), pref, upsert = true).void >>- {
      cache remove pref.id
      if (notifyChange) bus.publish(SendTo(pref.id, "prefChange", true), 'users)
    }

  def setPref(user: User, change: Pref => Pref, notifyChange: Boolean): Funit =
    getPref(user) map change flatMap { setPref(_, notifyChange) }

  def setPref(userId: String, change: Pref => Pref, notifyChange: Boolean): Funit =
    getPref(userId) map change flatMap { setPref(_, notifyChange) }

  def setPrefString(user: User, name: String, value: String, notifyChange: Boolean): Funit =
    getPref(user) map { _.set(name, value) } flatten
      s"Bad pref ${user.id} $name -> $value" flatMap { setPref(_, notifyChange) }
}
