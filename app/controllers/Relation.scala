package controllers

import play.api.libs.json.{ Json, Writes }
import scala.concurrent.duration.*

import lila.api.Context
import lila.app.{ given, * }
import lila.common.config.MaxPerSecond
import lila.common.paginator.{ AdapterLike, Paginator, PaginatorJson }
import lila.relation.Related
import lila.relation.RelationStream.*
import lila.user.{ User as UserModel }
import views.*
import lila.common.config

final class Relation(
    env: Env,
    apiC: => Api
) extends LilaController(env):

  val api = env.relation.api

  private def renderActions(username: UserStr, mini: Boolean)(implicit ctx: Context) =
    env.user.lightUserApi.asyncFallbackName(username) flatMap { user =>
      (ctx.userId ?? { api.fetchRelation(_, user.id) }) zip
        (ctx.isAuth ?? { env.pref.api followable user.id }) zip
        (ctx.userId ?? { api.fetchBlocks(user.id, _) }) flatMap { case ((relation, followable), blocked) =>
          negotiate(
            html = fuccess(Ok {
              if (mini)
                html.relation.mini(user.id, blocked = blocked, followable = followable, relation = relation)
              else
                html.relation.actions(user, relation = relation, blocked = blocked, followable = followable)
            }),
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

  private val FollowLimitPerUser = new lila.memo.RateLimit[UserId](
    credits = 150,
    duration = 72.hour,
    key = "follow.user"
  )

  def follow(username: UserStr) =
    Auth { implicit ctx => me =>
      FollowLimitPerUser(me.id) {
        api.reachedMaxFollowing(me.id) flatMap {
          case true =>
            env.msg.api
              .postPreset(
                me.id,
                lila.msg.MsgPreset.maxFollow(me.username, env.relation.maxFollow.value)
              ) inject Ok
          case _ =>
            api.follow(me.id, UserModel normalize username).recoverDefault >> renderActions(
              username,
              getBool("mini")
            )
        }
      }(rateLimitedFu)
    }

  def apiFollow(userId: UserId) =
    Scoped(_.Follow.Write) { _ => me =>
      FollowLimitPerUser[Fu[Api.ApiResult]](me.id) {
        api.reachedMaxFollowing(me.id) flatMap {
          case true =>
            fuccess(
              Api.ClientError(lila.msg.MsgPreset.maxFollow(me.username, env.relation.maxFollow.value).text)
            )
          case _ =>
            api.follow(me.id, UserModel normalize userId).recoverDefault inject Api.Done
        }
      }(fuccess(Api.Limited)) map apiC.toHttp
    }

  def unfollow(username: UserStr) =
    Auth { implicit ctx => me =>
      FollowLimitPerUser(me.id) {
        api.unfollow(me.id, UserModel normalize username).recoverDefault >> renderActions(
          username,
          getBool("mini")
        )
      }(rateLimitedFu)
    }

  def apiUnfollow(userId: UserId) =
    Scoped(_.Follow.Write) { _ => me =>
      FollowLimitPerUser[Fu[Api.ApiResult]](me.id) {
        api.unfollow(me.id, UserModel normalize userId) inject Api.Done
      }(fuccess(Api.Limited)) map apiC.toHttp
    }

  def block(username: UserStr) =
    Auth { implicit ctx => me =>
      FollowLimitPerUser(me.id) {
        api.block(me.id, UserModel normalize username).recoverDefault >> renderActions(
          username,
          getBool("mini")
        )
      }(rateLimitedFu)
    }

  def unblock(username: UserStr) =
    Auth { implicit ctx => me =>
      api.unblock(me.id, UserModel normalize username).recoverDefault >> renderActions(
        username,
        getBool("mini")
      )
    }

  def following(username: UserStr, page: Int) =
    Open { implicit ctx =>
      Reasonable(page, config.Max(20)) {
        OptionFuResult(env.user.repo byId username) { user =>
          RelatedPager(api.followingPaginatorAdapter(user.id), page) flatMap { pag =>
            negotiate(
              html = {
                if (ctx.is(user) || isGranted(_.CloseAccount))
                  Ok(html.relation.bits.friends(user, pag)).toFuccess
                else ctx.me.fold(notFound)(me => Redirect(routes.Relation.following(me.username)).toFuccess)
              },
              api = _ => Ok(jsonRelatedPaginator(pag)).toFuccess
            )
          }
        }
      }
    }

  def followers(username: UserStr, page: Int) =
    Open { implicit ctx =>
      negotiate(
        html = notFound,
        api = _ =>
          Reasonable(page, config.Max(20)) {
            RelatedPager(api.followersPaginatorAdapter(UserModel normalize username), page) flatMap { pag =>
              Ok(jsonRelatedPaginator(pag)).toFuccess
            }
          }
      )
    }

  def apiFollowing = Scoped(_.Follow.Read) { implicit req => me =>
    apiC.jsonStream {
      env.relation.stream
        .follow(me, Direction.Following, MaxPerSecond(30))
        .map(env.api.userApi.one)
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
        .add("online" -> env.socket.isOnline.value(r.user.id))
    }))

  def blocks(page: Int) =
    Auth { implicit ctx => me =>
      Reasonable(page, config.Max(20)) {
        RelatedPager(api.blockingPaginatorAdapter(me.id), page) map { pag =>
          html.relation.bits.blocks(me, pag)
        }
      }
    }

  private def RelatedPager(adapter: AdapterLike[String], page: Int)(implicit ctx: Context) =
    Paginator(
      adapter = adapter mapFutureList followship,
      currentPage = page,
      maxPerPage = lila.common.config.MaxPerPage(30)
    )

  private def followship(userIds: Seq[String])(implicit ctx: Context): Fu[List[Related]] =
    env.user.repo usersFromSecondary userIds.map(UserModel.normalize) flatMap { users =>
      (ctx.isAuth ?? { env.pref.api.followableIds(users map (_.id)) }) flatMap { followables =>
        users.map { u =>
          ctx.userId ?? { api.fetchRelation(_, u.id) } map { rel =>
            lila.relation.Related(u, none, followables(u.id), rel)
          }
        }.sequenceFu
      }
    }
