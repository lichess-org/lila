package controllers

import play.api.libs.json.{ Json, Writes }
import play.api.mvc.Result
import scalalib.Json.given
import scalalib.paginator.{ AdapterLike, Paginator }

import lila.app.{ *, given }
import lila.core.LightUser
import lila.core.perf.UserWithPerfs
import lila.rating.UserPerfsExt.bestRatedPerf
import lila.relation.Related
import lila.relation.RelationStream.*

final class Relation(env: Env, apiC: => Api) extends LilaController(env):

  val api = env.relation.api

  private def renderActions(username: UserName, mini: Boolean)(using ctx: Context) = for
    user <- env.user.lightUserApi.asyncFallbackName(username)
    relation <- ctx.userId.so(api.fetchRelation(_, user.id))
    followable <- ctx.isAuth.so(env.pref.api.followable(user.id))
    blocked <- ctx.userId.so(api.fetchBlocks(user.id, _))
    res <- Ok.snip:
      if mini
      then views.relation.mini(user.id, blocked = blocked, followable = followable, relation)
      else views.relation.actions(user, relation, blocked = blocked, followable = followable)
  yield res

  private def RatelimitWith(
      str: UserStr
  )(f: LightUser => Fu[Result])(using me: Me)(using Context): Fu[Result] =
    Found(env.user.lightUserApi.async(str.id)): user =>
      limit.follow(me, rateLimited):
        f(user)

  def follow(username: UserStr) = AuthOrScoped(_.Follow.Write, _.Web.Mobile) { ctx ?=> me ?=>
    RatelimitWith(username): user =>
      for
        reachedMax <- api.reachedMaxFollowing(me)
        res <-
          if reachedMax then
            val msg = lila.msg.MsgPreset.maxFollow(me.username, env.relation.maxFollow)
            for
              _ <- env.msg.api.postPreset(me, msg)
              res <- rateLimited(msg.name)
            yield res
          else
            for
              _ <- api.follow(me, user.id).recoverDefault
              res <- negotiate(renderActions(user.name, getBool("mini")), jsonOkResult)
            yield res
      yield res
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

  def block(username: UserStr) = AuthOrScoped(_.Follow.Write, _.Web.Mobile) { ctx ?=> me ?=>
    RatelimitWith(username): user =>
      api.block(me, user.id).recoverDefault >> negotiate(
        renderActions(user.name, getBool("mini")),
        jsonOkResult
      )
  }

  def unblock(username: UserStr) = AuthOrScoped(_.Follow.Write, _.Web.Mobile) { ctx ?=> me ?=>
    RatelimitWith(username): user =>
      api.unblock(me, user.id).recoverDefault >> negotiate(
        renderActions(user.name, getBool("mini")),
        jsonOkResult
      )
  }

  def following(username: UserStr, page: Int) = Open:
    Reasonable(page, Max(20)):
      Found(meOrFetch(username)): user =>
        for
          _ <- (page == 1).so(api.unfollowInactiveAccounts(user.id))
          pag <- RelatedPager(api.followingPaginatorAdapter(user.id), page)
          res <- negotiate(
            if ctx.is(user) || isGrantedOpt(_.CloseAccount)
            then Ok.page(views.relation.friends(user, pag))
            else Found(ctx.me)(me => Redirect(routes.Relation.following(me.username))),
            Ok(jsonRelatedPaginator(pag))
          )
        yield res

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
        .add("online" -> env.socket.isOnline.exec(r.user.id)))

  def blocks(page: Int) = Auth { ctx ?=> me ?=>
    Reasonable(page, Max(20)):
      Ok.async:
        RelatedPager(api.blockingPaginatorAdapter(me), page).map:
          views.relation.blocks(me, _)
  }

  private def RelatedPager(adapter: AdapterLike[UserId], page: Int)(using Context) =
    Paginator(
      adapter = adapter.mapFutureList(followship),
      currentPage = page,
      maxPerPage = MaxPerPage(30)
    )

  private def followship(userIds: Seq[UserId])(using ctx: Context): Fu[List[Related[UserWithPerfs]]] = for
    users <- env.user.api.listWithPerfs(userIds.toList)
    followables <- ctx.isAuth.so(env.pref.api.followableIds(users.map(_.id)))
    rels <- users.sequentially: u =>
      ctx.userId
        .so(api.fetchRelation(_, u.id))
        .map: rel =>
          lila.relation.Related(u, none, followables(u.id), rel)
  yield rels
