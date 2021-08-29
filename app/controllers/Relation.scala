package controllers

import play.api.libs.json.Json
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.common.config.MaxPerSecond
import lila.common.paginator.{ AdapterLike, Paginator, PaginatorJson }
import lila.relation.Related
import lila.relation.RelationStream._
import lila.user.{ User => UserModel }
import views._

final class Relation(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  val api = env.relation.api

  private def renderActions(userId: String, mini: Boolean)(implicit ctx: Context) =
    (ctx.userId ?? { api.fetchRelation(_, userId) }) zip
      (ctx.isAuth ?? { env.pref.api followable userId }) zip
      (ctx.userId ?? { api.fetchBlocks(userId, _) }) flatMap { case ((relation, followable), blocked) =>
        negotiate(
          html = fuccess(Ok {
            if (mini)
              html.relation.mini(userId, blocked = blocked, followable = followable, relation = relation)
            else
              html.relation.actions(userId, relation = relation, blocked = blocked, followable = followable)
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

  private val FollowLimitPerUser = new lila.memo.RateLimit[lila.user.User.ID](
    credits = 150,
    duration = 72.hour,
    key = "follow.user"
  )

  def follow(userId: String) =
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
            api.follow(me.id, UserModel normalize userId).nevermind >> renderActions(userId, getBool("mini"))
        }
      }(rateLimitedFu)
    }

  def apiFollow(userId: String) =
    Scoped(_.Follow.Write) { _ => me =>
      FollowLimitPerUser[Fu[Api.ApiResult]](me.id) {
        api.reachedMaxFollowing(me.id) flatMap {
          case true =>
            fuccess(
              Api.ClientError(lila.msg.MsgPreset.maxFollow(me.username, env.relation.maxFollow.value).text)
            )
          case _ =>
            api.follow(me.id, UserModel normalize userId).nevermind inject Api.Done
        }
      }(fuccess(Api.Limited)) map apiC.toHttp
    }

  def unfollow(userId: String) =
    Auth { implicit ctx => me =>
      FollowLimitPerUser(me.id) {
        api.unfollow(me.id, UserModel normalize userId).nevermind >> renderActions(userId, getBool("mini"))
      }(rateLimitedFu)
    }

  def apiUnfollow(userId: String) =
    Scoped(_.Follow.Write) { _ => me =>
      FollowLimitPerUser[Fu[Api.ApiResult]](me.id) {
        api.unfollow(me.id, UserModel normalize userId) inject Api.Done
      }(fuccess(Api.Limited)) map apiC.toHttp
    }

  def block(userId: String) =
    Auth { implicit ctx => me =>
      FollowLimitPerUser(me.id) {
        api.block(me.id, UserModel normalize userId).nevermind >> renderActions(userId, getBool("mini"))
      }(rateLimitedFu)
    }

  def unblock(userId: String) =
    Auth { implicit ctx => me =>
      api.unblock(me.id, UserModel normalize userId).nevermind >> renderActions(userId, getBool("mini"))
    }

  def following(username: String, page: Int) =
    Open { implicit ctx =>
      Reasonable(page, 20) {
        OptionFuResult(env.user.repo named username) { user =>
          RelatedPager(api.followingPaginatorAdapter(user.id), page) flatMap { pag =>
            negotiate(
              html = api countFollowers user.id map { nbFollowers =>
                Ok(html.relation.bits.following(user, pag, nbFollowers))
              },
              api = _ => Ok(jsonRelatedPaginator(pag)).fuccess
            )
          }
        }
      }
    }

  def followers(username: String, page: Int) =
    Open { implicit ctx =>
      Reasonable(page, 20) {
        OptionFuResult(env.user.repo named username) { user =>
          RelatedPager(api.followersPaginatorAdapter(user.id), page) flatMap { pag =>
            negotiate(
              html = api countFollowing user.id map { nbFollowing =>
                Ok(html.relation.bits.followers(user, pag, nbFollowing))
              },
              api = _ => Ok(jsonRelatedPaginator(pag)).fuccess
            )
          }
        }
      }
    }

  def apiFollowing(name: String) = apiRelation(name, Direction.Following)

  def apiFollowers(name: String) = apiRelation(name, Direction.Followers)

  private def apiRelation(name: String, direction: Direction) =
    Action.async { implicit req =>
      env.user.repo.named(name) flatMap {
        _ ?? { user =>
          apiC.jsonStream {
            env.relation.stream
              .follow(user, direction, MaxPerSecond(20))
              .map(env.api.userApi.one)
          }.fuccess
        }
      }
    }

  private def jsonRelatedPaginator(pag: Paginator[Related]) = {
    import lila.user.JsonView.nameWrites
    import lila.relation.JsonView.relatedWrites
    Json.obj("paginator" -> PaginatorJson(pag.mapResults { r =>
      relatedWrites.writes(r) ++ Json
        .obj(
          "perfs" -> r.user.perfs.bestPerfType.map { best =>
            lila.user.JsonView.perfs(r.user, best.some)
          }
        )
        .add("online" -> env.socket.isOnline(r.user.id))
    }))
  }

  def blocks(page: Int) =
    Auth { implicit ctx => me =>
      Reasonable(page, 20) {
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
}
