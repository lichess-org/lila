package controllers

import play.api.libs.iteratee._
import play.api.libs.json.Json
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.paginator.{ Paginator, AdapterLike, PaginatorJson }
import lila.common.{ HTTPRequest, MaxPerSecond }
import lila.relation.Related
import lila.relation.RelationStream._
import lila.user.{ User => UserModel, UserRepo }
import views._

object Relation extends LilaController {

  private def env = Env.relation

  private def renderActions(userId: String, mini: Boolean)(implicit ctx: Context) =
    (ctx.userId ?? { env.api.fetchRelation(_, userId) }) zip
      (ctx.isAuth ?? { Env.pref.api followable userId }) zip
      (ctx.userId ?? { env.api.fetchBlocks(userId, _) }) flatMap {
        case relation ~ followable ~ blocked => negotiate(
          html = fuccess(Ok {
            if (mini) html.relation.mini(userId, blocked = blocked, followable = followable, relation = relation)
            else html.relation.actions(userId, relation = relation, blocked = blocked, followable = followable)
          }),
          api = _ => fuccess(Ok(Json.obj(
            "followable" -> followable,
            "following" -> relation.contains(true),
            "blocking" -> relation.contains(false)
          )))
        )
      }

  def follow(userId: String) = Auth { implicit ctx => me =>
    env.api.reachedMaxFollowing(me.id) flatMap {
      case true => Env.message.api.sendPresetFromLichess(
        me,
        lila.message.ModPreset.maxFollow(me.username, Env.relation.MaxFollow)
      ).void
      case _ => env.api.follow(me.id, UserModel normalize userId).nevermind >> renderActions(userId, getBool("mini"))
    }
  }

  def unfollow(userId: String) = Auth { implicit ctx => me =>
    env.api.unfollow(me.id, UserModel normalize userId).nevermind >> renderActions(userId, getBool("mini"))
  }

  def block(userId: String) = Auth { implicit ctx => me =>
    env.api.block(me.id, UserModel normalize userId).nevermind >> renderActions(userId, getBool("mini"))
  }

  def unblock(userId: String) = Auth { implicit ctx => me =>
    env.api.unblock(me.id, UserModel normalize userId).nevermind >> renderActions(userId, getBool("mini"))
  }

  def following(username: String, page: Int) = Open { implicit ctx =>
    Reasonable(page, 20) {
      OptionFuResult(UserRepo named username) { user =>
        RelatedPager(env.api.followingPaginatorAdapter(user.id), page) flatMap { pag =>
          negotiate(
            html = env.api countFollowers user.id map { nbFollowers =>
              Ok(html.relation.bits.following(user, pag, nbFollowers))
            },
            api = _ => Ok(jsonRelatedPaginator(pag)).fuccess
          )
        }
      }
    }
  }

  def followers(username: String, page: Int) = Open { implicit ctx =>
    Reasonable(page, 20) {
      OptionFuResult(UserRepo named username) { user =>
        RelatedPager(env.api.followersPaginatorAdapter(user.id), page) flatMap { pag =>
          negotiate(
            html = env.api countFollowing user.id map { nbFollowing =>
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

  private def apiRelation(name: String, direction: Direction) = Action.async { req =>
    UserRepo.named(name) flatMap {
      _ ?? { user =>
        import Api.limitedDefault
        Api.GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
          Api.jsonStream {
            env.stream.follow(user, direction, MaxPerSecond(20)) &> Enumeratee.map(Env.api.userApi.one)
          } |> fuccess
        }
      }
    }
  }

  private def jsonRelatedPaginator(pag: Paginator[Related]) = {
    import lila.user.JsonView.nameWrites
    import lila.relation.JsonView.relatedWrites
    Json.obj("paginator" -> PaginatorJson(pag.mapResults { r =>
      relatedWrites.writes(r) ++ Json.obj(
        "perfs" -> r.user.perfs.bestPerfType.map { best =>
          lila.user.JsonView.perfs(r.user, best.some)
        }
      ).add("online" -> Env.user.isOnline(r.user.id))
    }))
  }

  def blocks(page: Int) = Auth { implicit ctx => me =>
    Reasonable(page, 20) {
      RelatedPager(env.api.blockingPaginatorAdapter(me.id), page) map { pag =>
        html.relation.bits.blocks(me, pag)
      }
    }
  }

  private def RelatedPager(adapter: AdapterLike[String], page: Int)(implicit ctx: Context) = Paginator(
    adapter = adapter mapFutureList followship,
    currentPage = page,
    maxPerPage = lila.common.MaxPerPage(30)
  )

  private def followship(userIds: Seq[String])(implicit ctx: Context): Fu[List[Related]] =
    UserRepo usersFromSecondary userIds.map(UserModel.normalize) flatMap { users =>
      (ctx.isAuth ?? { Env.pref.api.followableIds(users map (_.id)) }) flatMap { followables =>
        users.map { u =>
          ctx.userId ?? { env.api.fetchRelation(_, u.id) } map { rel =>
            lila.relation.Related(u, none, followables(u.id), rel)
          }
        }.sequenceFu
      }
    }
}
