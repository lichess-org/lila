package controllers

import play.api.mvc._
import play.api.templates.Html

import lila.api.Context
import lila.app._
import lila.user.{ User => UserModel, UserRepo }
import views._

object Relation extends LilaController {

  private def env = Env.relation

  private def renderActions(userId: String, mini: Boolean)(implicit ctx: Context) =
    (ctx.userId ?? { env.api.relation(_, userId) }) zip
      (ctx.userId ?? { env.api.blocks(userId, _) }) map {
        case (relation, blocked) => mini.fold(
          html.relation.mini(userId, relation = relation),
          html.relation.actions(userId, relation = relation, blocked = blocked))
      } map { Ok(_) }

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

  def following(username: String) = Open { implicit ctx =>
    OptionFuOk(UserRepo named username) { user =>
      env.api.following(user.id) flatMap UserRepo.byIds flatMap { users =>
        env.api nbFollowers user.id map { followers =>
          html.relation.following(user, users.sorted, followers)
        }
      }
    }
  }

  def followers(username: String) = Open { implicit ctx =>
    OptionFuOk(UserRepo named username) { user =>
      env.api.followers(user.id) flatMap UserRepo.byIds flatMap { users =>
        env.api nbFollowing user.id map { following =>
          html.relation.followers(user, users.sorted, following)
        }
      }
    }
  }

  def suggest(username: String) = Open { implicit ctx =>
    OptionFuOk(UserRepo named username) { user =>
      lila.game.BestOpponents(user.id, 50) zip
        env.api.onlinePopularUsers(20) map {
          case (opponents, popular) => popular.filterNot(user ==).foldLeft(opponents) {
            case (xs, x) => xs.exists(_._1 == x).fold(xs, xs :+ (x, 0))
          } |> { html.relation.suggest(user, _) }
        }
    }
  }
}
