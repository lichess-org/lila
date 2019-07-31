package lidraughts.pref

import play.api.mvc.RequestHeader
import reactivemongo.bson._
import scala.concurrent.duration.FiniteDuration

import lidraughts.db.BSON
import lidraughts.db.dsl._
import lidraughts.user.User

final class PrefApi(
    coll: Coll,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    cacheTtl: FiniteDuration
) {

  private def fetchPref(id: String): Fu[Option[Pref]] = coll.find($id(id)).uno[Pref]
  private val cache = asyncCache.multi(
    name = "pref.fetchPref",
    f = fetchPref,
    expireAfter = _.ExpireAfterAccess(cacheTtl)
  )

  private implicit val prefBSONHandler = new BSON[Pref] {

    import lidraughts.db.BSON.MapValue.{ MapReader, MapWriter }
    implicit val tagsReader = MapReader[String, String]
    implicit val tagsWriter = MapWriter[String, String]

    def reads(r: BSON.Reader): Pref = Pref(
      _id = r str "_id",
      dark = r.getD("dark", Pref.default.dark),
      transp = r.getD("transp", Pref.default.transp),
      bgImg = r.strO("bgImg"),
      theme = r.getD("theme", Pref.default.theme),
      pieceSet = r.getD("pieceSet", Pref.default.pieceSet),
      soundSet = r.getD("soundSet", Pref.default.soundSet),
      blindfold = r.getD("blindfold", Pref.default.blindfold),
      autoThreefold = r.getD("autoThreefold", Pref.default.autoThreefold),
      takeback = r.getD("takeback", Pref.default.takeback),
      moretime = r.getD("moretime", Pref.default.moretime),
      clockTenths = r.getD("clockTenths", Pref.default.clockTenths),
      clockBar = r.getD("clockBar", Pref.default.clockBar),
      clockSound = r.getD("clockSound", Pref.default.clockSound),
      premove = r.getD("premove", Pref.default.premove),
      animation = r.getD("animation", Pref.default.animation),
      captured = r.getD("captured", Pref.default.captured),
      kingMoves = r.getD("kingMoves", Pref.default.kingMoves),
      follow = r.getD("follow", Pref.default.follow),
      highlight = r.getD("highlight", Pref.default.highlight),
      destination = r.getD("destination", Pref.default.destination),
      coords = r.getD("coords", Pref.default.coords),
      replay = r.getD("replay", Pref.default.replay),
      gameResult = r.getD("gameResult", Pref.default.gameResult),
      challenge = r.getD("challenge", Pref.default.challenge),
      message = r.getD("message", Pref.default.message),
      studyInvite = r.getD("studyInvite", Pref.default.studyInvite),
      coordColor = r.getD("coordColor", Pref.default.coordColor),
      submitMove = r.getD("submitMove", Pref.default.submitMove),
      confirmResign = r.getD("confirmResign", Pref.default.confirmResign),
      insightShare = r.getD("insightShare", Pref.default.insightShare),
      keyboardMove = r.getD("keyboardMove", Pref.default.keyboardMove),
      fullCapture = r.getD("fullCapture", Pref.default.fullCapture),
      zen = r.getD("zen", Pref.default.zen),
      resizeHandle = r.getD("resizeHandle", Pref.default.resizeHandle),
      moveEvent = r.getD("moveEvent", Pref.default.moveEvent),
      puzzleVariant = r strO "puzzleVariant" flatMap draughts.variant.Variant.apply getOrElse draughts.variant.Standard,
      tags = r.getD("tags", Pref.default.tags)
    )

    def writes(w: BSON.Writer, o: Pref) = $doc(
      "_id" -> o._id,
      "dark" -> o.dark,
      "transp" -> o.transp,
      "bgImg" -> o.bgImg,
      "theme" -> o.theme,
      "pieceSet" -> o.pieceSet,
      "soundSet" -> SoundSet.name2key(o.soundSet),
      "blindfold" -> o.blindfold,
      "autoThreefold" -> o.autoThreefold,
      "takeback" -> o.takeback,
      "moretime" -> o.moretime,
      "clockTenths" -> o.clockTenths,
      "clockBar" -> o.clockBar,
      "clockSound" -> o.clockSound,
      "premove" -> o.premove,
      "animation" -> o.animation,
      "captured" -> o.captured,
      "kingMoves" -> o.kingMoves,
      "follow" -> o.follow,
      "highlight" -> o.highlight,
      "destination" -> o.destination,
      "coords" -> o.coords,
      "replay" -> o.replay,
      "gameResult" -> o.gameResult,
      "challenge" -> o.challenge,
      "message" -> o.message,
      "studyInvite" -> o.studyInvite,
      "coordColor" -> o.coordColor,
      "submitMove" -> o.submitMove,
      "confirmResign" -> o.confirmResign,
      "insightShare" -> o.insightShare,
      "keyboardMove" -> o.keyboardMove,
      "fullCapture" -> o.fullCapture,
      "zen" -> o.zen,
      "moveEvent" -> o.moveEvent,
      "puzzleVariant" -> o.puzzleVariant.key,
      "resizeHandle" -> o.resizeHandle,
      "tags" -> o.tags
    )
  }

  def saveTag(user: User, tag: Pref.Tag.type => String, value: String) =
    coll.update(
      $id(user.id),
      $set(s"tags.${tag(Pref.Tag)}" -> value),
      upsert = true
    ).void >>- { cache refresh user.id }

  def getPrefById(id: String): Fu[Pref] = cache get id dmap (_ getOrElse Pref.create(id))
  val getPref = getPrefById _
  def getPref(user: User): Fu[Pref] = getPref(user.id)
  def getPref(user: Option[User]): Fu[Pref] = user.fold(fuccess(Pref.default))(getPref)

  def getPref[A](user: User, pref: Pref => A): Fu[A] = getPref(user) map pref
  def getPref[A](userId: String, pref: Pref => A): Fu[A] = getPref(userId) map pref

  def getPref(user: User, req: RequestHeader): Fu[Pref] =
    getPref(user) map RequestPref.queryParamOverride(req)

  def followable(userId: String): Fu[Boolean] =
    coll.find($id(userId), $doc("follow" -> true)).uno[Bdoc] map {
      _ flatMap (_.getAs[Boolean]("follow")) getOrElse Pref.default.follow
    }

  def unfollowableIds(userIds: List[String]): Fu[Set[String]] =
    coll.distinct[String, Set]("_id", ($inIds(userIds) ++ $doc(
      "follow" -> false
    )).some)

  def followableIds(userIds: List[String]): Fu[Set[String]] =
    unfollowableIds(userIds) map userIds.toSet.diff

  def followables(userIds: List[String]): Fu[List[Boolean]] =
    followableIds(userIds) map { followables => userIds map followables.contains }

  def setPref(pref: Pref): Funit =
    coll.update($id(pref.id), pref, upsert = true).void >>- {
      cache refresh pref.id
    }

  def setPref(user: User, change: Pref => Pref): Funit =
    getPref(user) map change flatMap setPref

  def setPref(userId: String, change: Pref => Pref): Funit =
    getPref(userId) map change flatMap setPref

  def setPrefString(user: User, name: String, value: String): Funit =
    getPref(user) map { _.set(name, value) } flatten
      s"Bad pref ${user.id} $name -> $value" flatMap setPref

  def setBot(user: User): Funit =
    setPref(user, (p: Pref) => p.copy(
      takeback = Pref.Takeback.NEVER,
      moretime = Pref.Moretime.NEVER,
      insightShare = Pref.InsightShare.EVERYBODY
    ))
}
