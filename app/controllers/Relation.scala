package controllers

import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.common.paginator.{ Paginator, AdapterLike, PaginatorJson }
import lila.relation.Related
import lila.user.{ User => UserModel, UserRepo }
import views._

object Relation extends LilaController {

  private def env = Env.relation

  private def renderActions(userId: String, mini: Boolean)(implicit ctx: Context) =
    (ctx.userId ?? { env.api.fetchRelation(_, userId) }) zip
      (ctx.isAuth ?? { Env.pref.api followable userId }) zip
      (ctx.userId ?? { env.api.fetchBlocks(userId, _) }) flatMap {
        case ((relation, followable), blocked) => negotiate(
          html = fuccess(Ok(mini.fold(
            html.relation.mini(userId, blocked = blocked, followable = followable, relation = relation),
            html.relation.actions(userId, relation = relation, blocked = blocked, followable = followable)
          ))),
          api = _ => fuccess(Ok(Json.obj(
            "followable" -> followable,
            "following" -> relation.contains(true),
            "blocking" -> relation.contains(false)
          )))
        )
      }

  def follow(userId: String) = Auth { implicit ctx =>
    me =>
      env.api.follow(me.id, userId).nevermind >> renderActions(userId, getBool("mini"))
  }

  def unfollow(userId: String) = Auth { implicit ctx =>
    me =>
      env.api.unfollow(me.id, userId).nevermind >> renderActions(userId, getBool("mini"))
  }

  def block(userId: String) = Auth { implicit ctx =>
    me =>
      env.api.block(me.id, userId).nevermind >> renderActions(userId, getBool("mini"))
  }

  def unblock(userId: String) = Auth { implicit ctx =>
    me =>
      env.api.unblock(me.id, userId).nevermind >> renderActions(userId, getBool("mini"))
  }

  def following(username: String, page: Int) = Open { implicit ctx =>
    OptionFuResult(UserRepo named username) { user =>
      RelatedPager(env.api.followingPaginatorAdapter(user.id), page) flatMap { pag =>
        negotiate(
          html = env.api countFollowers user.id map { nbFollowers =>
            Ok(html.relation.following(user, pag, nbFollowers))
          },
          api = _ => Ok(jsonRelatedPaginator(pag)).fuccess)
      }
    }
  }

  def followers(username: String, page: Int) = Open { implicit ctx =>
    OptionFuResult(UserRepo named username) { user =>
      RelatedPager(env.api.followersPaginatorAdapter(user.id), page) flatMap { pag =>
        negotiate(
          html = env.api countFollowing user.id map { nbFollowing =>
            Ok(html.relation.followers(user, pag, nbFollowing))
          },
          api = _ => Ok(jsonRelatedPaginator(pag)).fuccess)
      }
    }
  }

  private def jsonRelatedPaginator(pag: Paginator[Related]) = {
    import lila.user.JsonView.nameWrites
    import lila.relation.JsonView.relatedWrites
    Json.obj("paginator" -> PaginatorJson(pag))
  }

  def blocks(page: Int) = Auth { implicit ctx =>
    me =>
      RelatedPager(env.api.blockingPaginatorAdapter(me.id), page) map { pag =>
        html.relation.blocks(me, pag)
      }
  }

  private def RelatedPager(adapter: AdapterLike[String], page: Int)(implicit ctx: Context) = Paginator(
    adapter = adapter mapFutureList followship,
    currentPage = page,
    maxPerPage = 30)

  private def followship(userIds: Seq[String])(implicit ctx: Context): Fu[List[Related]] =
    UserRepo byIds userIds flatMap { users =>
      (ctx.isAuth ?? { Env.pref.api.followableIds(users map (_.id)) }) flatMap { followables =>
        users.map { u =>
          ctx.userId ?? { env.api.fetchRelation(_, u.id) } map { rel =>
            lila.relation.Related(u, none, followables(u.id), rel)
          }
        }.sequenceFu
      }
    }
}
