package controllers

import play.api.libs.json.{ Json, Writes }
import play.api.mvc.Result
import scalalib.Json.given

import lila.app.{ *, given }
import scalalib.paginator.{ AdapterLike, Paginator }
import lila.core.LightUser
import lila.relation.Related
import lila.relation.RelationStream.*
import lila.core.perf.UserWithPerfs
import lila.rating.UserPerfsExt.bestRatedPerf

final class Relation(env: Env, apiC: => Api) extends LilaController(env):

  val api = env.relation.api

  private def renderActions(username: UserName, mini: Boolean)(using ctx: Context) = for
    user       <- env.user.lightUserApi.asyncFallbackName(username)
    relation   <- ctx.userId.so(api.fetchRelation(_, user.id))
    followable <- ctx.isAuth.so(env.pref.api.followable(user.id))
    blocked    <- ctx.userId.so(api.fetchBlocks(user.id, _))
    res <- negotiate(
      Ok.page:
        if mini
        then views.relation.mini(user.id, blocked = blocked, followable = followable, relation)
        else views.relation.actions(user, relation, blocked = blocked, followable = followable)
      ,
      Ok:
        Json.obj(
          "followable" -> followable,
          "following"  -> relation.contains(true),
          "blocking"   -> relation.contains(false)
        )
    )
  yield res

  private val FollowLimitPerUser = lila.memo.RateLimit[UserId](
    credits = 150,
    duration = 72.hour,
    key = "follow.user"
  )

  private def RatelimitWith(
      str: UserStr
  )(f: LightUser => Fu[Result])(using me: Me)(using Context): Fu[Result] =
    Found(env.user.lightUserApi.async(str.id)): user =>
      FollowLimitPerUser(me, rateLimited):
        f(user)

  def follow(username: UserStr) = AuthOrScoped(_.Follow.Write, _.Web.Mobile) { ctx ?=> me ?=>
    RatelimitWith(username): user =>
      api.reachedMaxFollowing(me).flatMap {
        if _ then
          val msg = lila.msg.MsgPreset.maxFollow(me.username, env.relation.maxFollow.value)
          env.msg.api.postPreset(me, msg) >> rateLimited(msg.name)
        else
          api.follow(me, user.id).recoverDefault >> negotiate(
            renderActions(user.name, getBool("mini")),
            jsonOkResult
          )
      }
  }
  def followBc = follow

  def unfollow(username: UserStr) = AuthOrScoped(_.Follow.Write, _.Web.Mobile) { ctx ?=> me ?=>
    RatelimitWith(username): user =>
      api.unfollow(me, user.id).recoverDefault >> negotiate(
        renderActions(user.name, getBool("mini")),
        jsonOkResult
      )
  }
  def unfollowBc = unfollow

  def block(username: UserStr) = Auth { ctx ?=> me ?=>
    RatelimitWith(username): user =>
      api.block(me, user.id).recoverDefault >> renderActions(user.name, getBool("mini"))
  }

  def unblock(username: UserStr) = Auth { ctx ?=> me ?=>
    RatelimitWith(username): user =>
      api.unblock(me, user.id).recoverDefault >> renderActions(user.name, getBool("mini"))
  }

  def following(username: UserStr, page: Int) = Open:
    Reasonable(page, Max(20)):
      Found(meOrFetch(username)): user =>
        RelatedPager(api.followingPaginatorAdapter(user.id), page).flatMap: pag =>
          negotiate(
            if ctx.is(user) || isGrantedOpt(_.CloseAccount)
            then Ok.page(views.relation.bits.friends(user, pag))
            else Found(ctx.me)(me => Redirect(routes.Relation.following(me.username))),
            Ok(jsonRelatedPaginator(pag))
          )

  def followers(username: UserStr, page: Int) = Open:
    negotiateJson:
      Reasonable(page, Max(20)):
        RelatedPager(api.followersPaginatorAdapter(username.id), page).flatMap: pag =>
          Ok(jsonRelatedPaginator(pag))

  def apiFollowing = Scoped(_.Follow.Read, _.Web.Mobile) { ctx ?=> me ?=>
    apiC.jsonDownload:
      env.relation.stream
        .follow(me, Direction.Following, MaxPerSecond(30))
        .mapAsync(1): ids =>
          env.user.api.listWithPerfs(ids.toList)
        .mapConcat(identity)
        .map(env.api.userApi.one(_, None))
  }

  private def jsonRelatedPaginator(pag: Paginator[Related[UserWithPerfs]]) =
    import lila.common.Json.{ *, given }
    given Writes[UserWithPerfs] = writeAs(_.user.light)
    import lila.relation.JsonView.given
    Json.obj("paginator" -> pag.mapResults: r =>
      Json.toJsObject(r) ++ Json
        .obj:
          "perfs" -> r.user.perfs.bestRatedPerf.map:
            lila.user.JsonView.keyedPerfJson
        .add("online" -> env.socket.isOnline(r.user.id)))

  def blocks(page: Int) = Auth { ctx ?=> me ?=>
    Reasonable(page, Max(20)):
      Ok.pageAsync:
        RelatedPager(api.blockingPaginatorAdapter(me), page).map {
          views.relation.bits.blocks(me, _)
        }
  }

  private def RelatedPager(adapter: AdapterLike[UserId], page: Int)(using Context) =
    Paginator(
      adapter = adapter.mapFutureList(followship),
      currentPage = page,
      maxPerPage = MaxPerPage(30)
    )

  private def followship(userIds: Seq[UserId])(using ctx: Context): Fu[List[Related[UserWithPerfs]]] = for
    users       <- env.user.api.listWithPerfs(userIds.toList)
    followables <- ctx.isAuth.so(env.pref.api.followableIds(users.map(_.id)))
    rels <- users.traverse: u =>
      ctx.userId
        .so(api.fetchRelation(_, u.id))
        .map: rel =>
          lila.relation.Related(u, none, followables(u.id), rel)
  yield rels
