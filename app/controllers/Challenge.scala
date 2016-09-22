package controllers

import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import play.api.mvc.{ Result, Cookie }
import scala.concurrent.duration._

import lila.api.{ Context, BodyContext }
import lila.app._
import lila.challenge.{ Challenge => ChallengeModel }
import lila.common.{ HTTPRequest, LilaCookie }
import lila.game.{ Pov, GameRepo, AnonCookie }
import lila.user.UserRepo
import views.html

object Challenge extends LilaController {

  private def env = Env.challenge

  def all = Auth { implicit ctx =>
    me =>
      env.api allFor me.id map { all =>
        Ok {
          env.jsonView(all) + (
            "i18n" -> translations
          )
        } as JSON
      }
  }

  def show(id: String) = Open { implicit ctx =>
    showId(id)
  }

  private def translations(implicit ctx: Context) = Env.i18n.jsDump.keysToObject(List(
    Env.i18n.keys.waiting
  ), Env.i18n.pool lang ctx.req)

  protected[controllers] def showId(id: String)(implicit ctx: Context): Fu[Result] =
    OptionFuResult(env.api byId id)(showChallenge)

  protected[controllers] def showChallenge(c: ChallengeModel)(implicit ctx: Context): Fu[Result] =
    env version c.id flatMap { version =>
      val mine = isMine(c)
      import lila.challenge.Direction
      val direction: Option[Direction] =
        if (mine) Direction.Out.some
        else if (isForMe(c)) Direction.In.some
        else none
      val json = env.jsonView.show(c, version, direction)
      negotiate(
        html = fuccess {
          Ok(mine.fold(html.challenge.mine.apply _, html.challenge.theirs.apply _)(c, json))
        },
        api = _ => Ok(json).fuccess
      ) flatMap withChallengeAnonCookie(mine && c.challengerIsAnon, c, true)
    }

  private def isMine(challenge: ChallengeModel)(implicit ctx: Context) = challenge.challenger match {
    case Left(anon)  => HTTPRequest sid ctx.req contains anon.secret
    case Right(user) => ctx.userId contains user.id
  }

  private def isForMe(challenge: ChallengeModel)(implicit ctx: Context) =
    challenge.destUserId.fold(true)(ctx.userId.contains)

  def accept(id: String) = Open { implicit ctx =>
    OptionFuResult(env.api byId id) { c =>
      isForMe(c) ?? env.api.accept(c, ctx.me).flatMap {
        case Some(pov) => negotiate(
          html = Redirect(routes.Round.watcher(pov.game.id, "white")).fuccess,
          api = apiVersion => Env.api.roundApi.player(pov, apiVersion) map { Ok(_) }
        ) flatMap withChallengeAnonCookie(ctx.isAnon, c, false)
        case None => negotiate(
          html = Redirect(routes.Round.watcher(c.id, "white")).fuccess,
          api = _ => notFoundJson("Someone else accepted the challenge"))
      }
    }
  }

  private def withChallengeAnonCookie(cond: Boolean, c: ChallengeModel, owner: Boolean)(res: Result)(implicit ctx: Context): Fu[Result] =
    cond ?? {
      GameRepo.game(c.id).map {
        _ map { game =>
          implicit val req = ctx.req
          LilaCookie.cookie(
            AnonCookie.name,
            game.player(owner.fold(c.finalColor, !c.finalColor)).id,
            maxAge = AnonCookie.maxAge.some,
            httpOnly = false.some)
        }
      }
    } map { cookieOption =>
      cookieOption.fold(res) { res.withCookies(_) }
    }

  def decline(id: String) = Auth { implicit ctx =>
    me =>
      OptionFuResult(env.api byId id) { c =>
        if (isForMe(c)) env.api decline c
        else notFound
      }
  }

  def cancel(id: String) = Open { implicit ctx =>
    OptionFuResult(env.api byId id) { c =>
      if (isMine(c)) env.api cancel c
      else notFound
    }
  }

  def rematchOf(gameId: String) = Auth { implicit ctx =>
    me =>
      OptionFuResult(GameRepo game gameId) { g =>
        Pov.opponentOfUserId(g, me.id).flatMap(_.userId) ?? UserRepo.byId flatMap {
          _ ?? { opponent =>
            restriction(opponent) flatMap {
              case Some(r) => BadRequest(jsonError(r.replace("{{user}}", opponent.username))).fuccess
              case _ => env.api.rematchOf(g, me) map {
                _.fold(Ok, BadRequest(jsonError("Sorry, couldn't create the rematch.")))
              }
            }
          }
        }
      }
  }

  def restriction(user: lila.user.User)(implicit ctx: Context): Fu[Option[String]] = ctx.me match {
    case None => fuccess("Only registered players can send challenges.".some)
    case Some(me) => Env.relation.api.fetchBlocks(user.id, me.id) flatMap {
      case true => fuccess(s"{{user}} doesn't accept challenges from you.".some)
      case false => Env.pref.api getPref user zip Env.relation.api.fetchFollows(user.id, me.id) map {
        case (pref, follow) => lila.pref.Pref.Challenge.block(me, user, pref.challenge, follow,
          fromCheat = me.engine && !user.engine)
      }
    }
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    if (!ctx.sameOrigin) fuccess(none)
    else env.api byId id flatMap {
      _ ?? { c =>
        get("sri") ?? { uid =>
          env.socketHandler.join(id, uid, ctx.userId, ctx.sameOrigin, isMine(c))
        }
      }
    }
  }
}
