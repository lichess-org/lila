package lila.pref

import play.api.mvc.RequestHeader
import reactivemongo.api.bson.*

import lila.db.dsl.{ given, * }
import lila.memo.CacheApi.*
import lila.user.User

final class PrefApi(
    val coll: Coll,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import PrefHandlers.given

  private def fetchPref(id: UserId): Fu[Option[Pref]] = coll.find($id(id)).one[Pref]

  private val cache = cacheApi[UserId, Option[Pref]](200_000, "pref.fetchPref"):
    _.expireAfterAccess(10 minutes).buildAsyncFuture(fetchPref)

  export cache.{ get as getPrefById }

  def saveTag(user: User, tag: Pref.Tag.type => String, value: Boolean) = {
    if value then
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
        .void andDo { cache invalidate user.id }
  } andDo { cache invalidate user.id }

  def get(user: User): Fu[Pref] = cache get user.id dmap {
    _ getOrElse Pref.create(user)
  }

  def get[A](user: User, pref: Pref => A): Fu[A] = get(user) dmap pref

  def get[A](userId: UserId, pref: Pref => A): Fu[A] =
    getPrefById(userId).dmap(p => pref(p | Pref.default))

  def get(user: User, req: RequestHeader): Fu[Pref] =
    get(user) dmap RequestPref.queryParamOverride(req)

  def get(user: Option[User], req: RequestHeader): Fu[Pref] = user match
    case Some(u) => get(u) dmap RequestPref.queryParamOverride(req)
    case None    => fuccess(RequestPref.fromRequest(req))

  def followable(userId: UserId): Fu[Boolean] =
    coll.primitiveOne[Boolean]($id(userId), "follow") map (_ | Pref.default.follow)

  private def unfollowableIds(userIds: List[UserId]): Fu[Set[UserId]] =
    coll.secondaryPreferred.distinctEasy[UserId, Set](
      "_id",
      $inIds(userIds) ++ $doc("follow" -> false)
    )

  def followableIds(userIds: List[UserId]): Fu[Set[UserId]] =
    unfollowableIds(userIds) map userIds.toSet.diff

  def followables(userIds: List[UserId]): Fu[List[Boolean]] =
    followableIds(userIds) map { followables =>
      userIds map followables.contains
    }

  private def unmentionableIds(userIds: Set[UserId]): Fu[Set[UserId]] =
    coll.secondaryPreferred.distinctEasy[UserId, Set](
      "_id",
      $inIds(userIds) ++ $doc("mention" -> false)
    )

  def mentionableIds(userIds: Set[UserId]): Fu[Set[UserId]] =
    unmentionableIds(userIds) map userIds.diff

  def setPref(pref: Pref): Funit =
    coll.update.one($id(pref.id), pref, upsert = true).void andDo
      cache.put(pref.id, fuccess(pref.some))

  def setPref(user: User, change: Pref => Pref): Funit =
    get(user) map change flatMap setPref

  def agree(user: User): Funit =
    coll.update.one($id(user.id), $set("agreement" -> Pref.Agreement.current), upsert = true).void andDo
      cache.invalidate(user.id)

  def setBot(user: User): Funit = setPref(
    user,
    _.copy(
      takeback = Pref.Takeback.NEVER,
      moretime = Pref.Moretime.NEVER,
      insightShare = Pref.InsightShare.EVERYBODY
    )
  )

  def saveNewUserPrefs(user: User, req: RequestHeader): Funit =
    val reqPref = RequestPref fromRequest req
    (reqPref != Pref.default) so setPref(reqPref.copy(_id = user.id))
