package controllers

import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.common.paginator.{ Paginator, AdapterLike }
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
    OptionFuOk(UserRepo named username) { user =>
      env.api countFollowers user.id flatMap { nbFollowers =>
        RelatedPager(env.api.followingPaginatorAdapter(user.id), page) map { pag =>
          html.relation.following(user, pag, nbFollowers)
        }
      }
    }
  }

  def followers(username: String, page: Int) = Open { implicit ctx =>
    OptionFuOk(UserRepo named username) { user =>
      env.api countFollowing user.id flatMap { nbFollowing =>
        RelatedPager(env.api.followersPaginatorAdapter(user.id), page) map { pag =>
          html.relation.followers(user, pag, nbFollowing)
        }
      }
    }
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
            lila.relation.Related(u, 0, followables(u.id), rel)
          }
        }.sequenceFu
      }
    }
}
