package controllers

import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.relation.Related
import lila.user.{ User => UserModel, UserRepo }
import views._

object Relation extends LilaController {

  private def env = Env.relation

  private def renderActions(userId: String, mini: Boolean)(implicit ctx: Context) =
    (ctx.userId ?? { env.api.relation(_, userId) }) zip
      (ctx.isAuth ?? { Env.pref.api followable userId }) zip
      (ctx.userId ?? { env.api.blocks(userId, _) }) flatMap {
        case ((relation, followable), blocked) => negotiate(
          html = fuccess(Ok(mini.fold(
            html.relation.mini(userId, blocked = blocked, followable = followable, relation = relation),
            html.relation.actions(userId, relation = relation, blocked = blocked, followable = followable)
          ))),
          api = _ => fuccess(Ok(Json.obj(
            "followable" -> followable,
            "following" -> relation.exists(true ==),
            "blocking" -> relation.exists(false ==)
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

  def following(username: String) = Open { implicit ctx =>
    OptionFuOk(UserRepo named username) { user =>
      env.api.following(user.id) flatMap followship flatMap { rels =>
        env.api nbFollowers user.id map { followers =>
          html.relation.following(user, rels, followers)
        }
      }
    }
  }

  def followers(username: String) = Open { implicit ctx =>
    OptionFuOk(UserRepo named username) { user =>
      env.api.followers(user.id) flatMap followship flatMap { rels =>
        env.api nbFollowing user.id map { following =>
          html.relation.followers(user, rels, following)
        }
      }
    }
  }

  def blocks = Auth { implicit ctx =>
    me =>
      env.api.blocking(me.id) flatMap followship map { rels =>
        html.relation.blocks(me, rels)
      }
  }

  private def followship(userIds: Set[String])(implicit ctx: Context): Fu[List[Related]] =
    UserRepo byIds userIds flatMap { users =>
      (ctx.isAuth ?? { Env.pref.api.followableIds(users map (_.id)) }) flatMap { followables =>
        users.map { u =>
          ctx.userId ?? { env.api.relation(_, u.id) } map { rel =>
            lila.relation.Related(u, 0, followables(u.id), rel)
          }
        }.sequenceFu
      }
    }

  def suggest(username: String) = Open { implicit ctx =>
    OptionFuResult(UserRepo named username) { user =>
      lila.game.BestOpponents(user.id, 50) flatMap { opponents =>
        Env.pref.api.followableIds(opponents map (_._1.id)) zip
          env.api.onlinePopularUsers(20) flatMap {
            case (followables, popular) =>
              popular.filterNot(user ==).foldLeft(opponents filter {
                case (u, _) => followables contains u.id
              }) {
                case (xs, x) => xs.exists(_._1 == x).fold(xs, xs :+ (x, 0))
              }.map {
                case (u, nb) => env.api.relation(user.id, u.id) map {
                  lila.relation.Related(u, nb, true, _)
                }
              }.sequenceFu flatMap { rels =>
                negotiate(
                  html = fuccess(Ok(html.relation.suggest(user, rels))),
                  api = _ => fuccess {
                    implicit val userWrites = play.api.libs.json.Writes[UserModel] { Env.user.jsonView(_, true) }
                    Ok(Json.obj(
                      "user" -> user,
                      "suggested" -> play.api.libs.json.JsArray(rels.map(_.toJson))))
                  })
              }
          }
      }
    }
  }
}
