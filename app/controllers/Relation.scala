package controllers

import play.api.libs.json.{ Json, Writes }
import play.api.mvc.Result

import lila.api.Context
import lila.app.{ given, * }
import lila.common.config.MaxPerSecond
import lila.common.paginator.{ AdapterLike, Paginator, PaginatorJson }
import lila.relation.Related
import lila.relation.RelationStream.*
import lila.user.{ User as UserModel }
import views.*
import lila.common.config
import Api.ApiResult
import lila.common.LightUser

final class Relation(env: Env, apiC: => Api) extends LilaController(env):

  val api = env.relation.api

  private def renderActions(username: UserName, mini: Boolean)(using ctx: Context) =
    env.user.lightUserApi.asyncFallbackName(username) flatMap { user =>
      (ctx.userId ?? { api.fetchRelation(_, user.id) }) zip
        (ctx.isAuth ?? { env.pref.api followable user.id }) zip
        (ctx.userId ?? { api.fetchBlocks(user.id, _) }) flatMap { case ((relation, followable), blocked) =>
          negotiate(
            html = fuccess(Ok:
              if mini then
                html.relation.mini(user.id, blocked = blocked, followable = followable, relation = relation)
              else
                html.relation.actions(user, relation = relation, blocked = blocked, followable = followable)
            ),
            api = _ =>
              fuccess(
                Ok(
                  Json.obj(
                    "followable" -> followable,
                    "following"  -> relation.contains(true),
                    "blocking"   -> relation.contains(false)
                  )
                )
              )
          )
        }
    }

  private val FollowLimitPerUser = lila.memo.RateLimit[UserId](
    credits = 150,
    duration = 72.hour,
    key = "follow.user"
  )

  private def FollowingUser(me: UserModel, str: UserStr)(f: LightUser => Fu[Result]): Fu[Result] =
    env.user.lightUserApi.async(str.id) flatMapz { user =>
      FollowLimitPerUser(me.id, rateLimitedFu):
        f(user)
    }

  def follow(username: UserStr) = Auth { ctx ?=> me =>
    FollowingUser(me, username) { user =>
      api.reachedMaxFollowing(me.id) flatMap {
        if _ then
          env.msg.api
            .postPreset(
              me.id,
              lila.msg.MsgPreset.maxFollow(me.username, env.relation.maxFollow.value)
            ) inject Ok
        else api.follow(me.id, user.id).recoverDefault >> renderActions(user.name, getBool("mini"))
      }
    }
  }

  def apiFollow(userId: UserStr) = Scoped(_.Follow.Write) { _ ?=> me =>
    FollowLimitPerUser(me.id, fuccess(ApiResult.Limited)):
      api
        .reachedMaxFollowing(me.id)
        .flatMap:
          if _ then
            fuccess:
              ApiResult.ClientError(
                lila.msg.MsgPreset.maxFollow(me.username, env.relation.maxFollow.value).text
              )
          else api.follow(me.id, userId.id).recoverDefault inject ApiResult.Done
    .map(apiC.toHttp)
  }

  def unfollow(username: UserStr) = Auth { ctx ?=> me =>
    FollowingUser(me, username): user =>
      api.unfollow(me.id, user.id).recoverDefault >> renderActions(user.name, getBool("mini"))
  }

  def apiUnfollow(userId: UserStr) = Scoped(_.Follow.Write) { _ ?=> me =>
    FollowLimitPerUser(me.id, fuccess(ApiResult.Limited)):
      api.unfollow(me.id, userId.id) inject ApiResult.Done
    .map(apiC.toHttp)
  }

  def block(username: UserStr) = Auth { ctx ?=> me =>
    FollowingUser(me, username) { user =>
      api.block(me.id, user.id).recoverDefault >> renderActions(user.name, getBool("mini"))
    }
  }

  def unblock(username: UserStr) = Auth { ctx ?=> me =>
    FollowingUser(me, username) { user =>
      api.unblock(me.id, user.id).recoverDefault >> renderActions(user.name, getBool("mini"))
    }
  }

  def following(username: UserStr, page: Int) = Open:
    Reasonable(page, config.Max(20)):
      OptionFuResult(env.user.repo byId username): user =>
        RelatedPager(api.followingPaginatorAdapter(user.id), page) flatMap { pag =>
          negotiate(
            html =
              if ctx.is(user) || isGranted(_.CloseAccount)
              then Ok(html.relation.bits.friends(user, pag)).toFuccess
              else ctx.me.fold(notFound)(me => Redirect(routes.Relation.following(me.username)).toFuccess),
            api = _ => Ok(jsonRelatedPaginator(pag)).toFuccess
          )
        }

  def followers(username: UserStr, page: Int) = Open:
    negotiate(
      html = notFound,
      api = _ =>
        Reasonable(page, config.Max(20)) {
          RelatedPager(api.followersPaginatorAdapter(username.id), page) flatMap { pag =>
            Ok(jsonRelatedPaginator(pag)).toFuccess
          }
        }
    )

  def apiFollowing = Scoped(_.Follow.Read) { req ?=> me =>
    apiC.jsonDownload {
      env.relation.stream
        .follow(me, Direction.Following, MaxPerSecond(30))
        .map(env.api.userApi.one(_, None))
    }.toFuccess
  }

  private def jsonRelatedPaginator(pag: Paginator[Related]) =
    given Writes[UserModel] = lila.user.JsonView.nameWrites
    import lila.relation.JsonView.given
    Json.obj("paginator" -> PaginatorJson(pag.mapResults { r =>
      Json.toJsObject(r) ++ Json
        .obj(
          "perfs" -> r.user.perfs.bestPerfType.map { best =>
            lila.user.JsonView.perfs(r.user, best.some)
          }
        )
        .add("online" -> env.socket.isOnline(r.user.id))
    }))

  def blocks(page: Int) = Auth { ctx ?=> me =>
    Reasonable(page, config.Max(20)) {
      RelatedPager(api.blockingPaginatorAdapter(me.id), page) map { pag =>
        html.relation.bits.blocks(me, pag)
      }
    }
  }

  private def RelatedPager(adapter: AdapterLike[UserId], page: Int)(using Context) =
    Paginator(
      adapter = adapter mapFutureList followship,
      currentPage = page,
      maxPerPage = lila.common.config.MaxPerPage(30)
    )

  private def followship(userIds: Seq[UserId])(using ctx: Context): Fu[List[Related]] =
    env.user.repo usersFromSecondary userIds flatMap { users =>
      (ctx.isAuth ?? { env.pref.api.followableIds(users map (_.id)) }) flatMap { followables =>
        users.map { u =>
          ctx.userId ?? { api.fetchRelation(_, u.id) } map { rel =>
            lila.relation.Related(u, none, followables(u.id), rel)
          }
        }.parallel
      }
    }
