package lila.pref

import play.api.mvc.RequestHeader
import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.CacheApi._
import lila.user.User

final class PrefApi(
    coll: Coll,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import PrefHandlers._

  private def fetchPref(id: User.ID): Fu[Option[Pref]] = coll.find($id(id)).one[Pref]

  private val cache = cacheApi[User.ID, Option[Pref]](65536, "pref.fetchPref") {
    _.expireAfterAccess(10 minutes)
      .buildAsyncFuture(fetchPref)
  }

  def saveTag(user: User, tag: Pref.Tag.type => String, value: Boolean) = {
    if (value)
      coll.update
        .one(
          $id(user.id),
          $set(s"tags.${tag(Pref.Tag)}" -> "1"),
          upsert = true
        )
        .void
    else
      coll.update
        .one($id(user.id), $unset(s"tags.${tag(Pref.Tag)}"))
        .void >>- { cache invalidate user.id }
  } >>- { cache invalidate user.id }

  def getPrefById(id: User.ID): Fu[Pref]    = cache get id dmap (_ getOrElse Pref.create(id))
  val getPref                               = getPrefById _
  def getPref(user: User): Fu[Pref]         = getPref(user.id)
  def getPref(user: Option[User]): Fu[Pref] = user.fold(fuccess(Pref.default))(getPref)

  def getPref[A](user: User, pref: Pref => A): Fu[A]      = getPref(user) dmap pref
  def getPref[A](userId: User.ID, pref: Pref => A): Fu[A] = getPref(userId) dmap pref

  def getPref(user: User, req: RequestHeader): Fu[Pref] =
    getPref(user) dmap RequestPref.queryParamOverride(req)

  def followable(userId: User.ID): Fu[Boolean] =
    coll.primitiveOne[Boolean]($id(userId), "follow") map (_ | Pref.default.follow)

  private def unfollowableIds(userIds: List[User.ID]): Fu[Set[User.ID]] =
    coll.secondaryPreferred.distinctEasy[User.ID, Set](
      "_id",
      $inIds(userIds) ++ $doc("follow" -> false)
    )

  def followableIds(userIds: List[User.ID]): Fu[Set[User.ID]] =
    unfollowableIds(userIds) map userIds.toSet.diff

  def followables(userIds: List[User.ID]): Fu[List[Boolean]] =
    followableIds(userIds) map { followables =>
      userIds map followables.contains
    }

  private def unmentionableIds(userIds: Set[User.ID]): Fu[Set[User.ID]] =
    coll.secondaryPreferred.distinctEasy[User.ID, Set](
      "_id",
      $inIds(userIds) ++ $doc("mention" -> false)
    )

  def mentionableIds(userIds: Set[User.ID]): Fu[Set[User.ID]] =
    unmentionableIds(userIds) map userIds.diff

  def setPref(pref: Pref): Funit =
    coll.update.one($id(pref.id), pref, upsert = true).void >>-
      cache.put(pref.id, fuccess(pref.some))

  def setPref(user: User, change: Pref => Pref): Funit =
    getPref(user) map change flatMap setPref

  def setPref(userId: User.ID, change: Pref => Pref): Funit =
    getPref(userId) map change flatMap setPref

  def setPrefString(user: User, name: String, value: String): Funit =
    getPref(user) map { _.set(name, value) } orFail
      s"Bad pref ${user.id} $name -> $value" flatMap setPref

  def setBot(user: User): Funit =
    setPref(
      user,
      (p: Pref) =>
        p.copy(
          takeback = Pref.Takeback.NEVER,
          moretime = Pref.Moretime.NEVER,
          insightShare = Pref.InsightShare.EVERYBODY
        )
    )

  def saveNewUserPrefs(user: User, req: RequestHeader): Funit = {
    val reqPref = RequestPref fromRequest req
    (reqPref != Pref.default) ?? setPref(reqPref.copy(_id = user.id))
  }
}
