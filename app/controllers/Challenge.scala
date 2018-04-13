package controllers

import play.api.libs.json._
import play.api.mvc.Result

import lila.api.Context
import lila.app._
import lila.challenge.{ Challenge => ChallengeModel }
import lila.common.{ HTTPRequest, LilaCookie }
import lila.game.{ Pov, GameRepo, AnonCookie }
import lila.user.UserRepo
import views.html

object Challenge extends LilaController {

  private def env = Env.challenge

  def all = Auth { implicit ctx => me =>
    XhrOrRedirectHome {
      env.api allFor me.id map { all =>
        Ok(env.jsonView(all, ctx.lang)) as JSON
      }
    }
  }

  def show(id: String) = Open { implicit ctx =>
    showId(id)
  }

  protected[controllers] def showId(id: String)(implicit ctx: Context): Fu[Result] =
    OptionFuResult(env.api byId id)(showChallenge(_))

  protected[controllers] def showChallenge(c: ChallengeModel, error: Option[String] = None)(implicit ctx: Context): Fu[Result] =
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
          if (mine) error match {
            case Some(e) => BadRequest(html.challenge.mine.apply(c, json, e.some))
            case None => Ok(html.challenge.mine.apply(c, json, none))
          }
          else Ok(html.challenge.theirs.apply(c, json))
        },
        api = _ => Ok(json).fuccess
      ) flatMap withChallengeAnonCookie(mine && c.challengerIsAnon, c, true)
    }

  private def isMine(challenge: ChallengeModel)(implicit ctx: Context) = challenge.challenger match {
    case Left(anon) => HTTPRequest sid ctx.req contains anon.secret
    case Right(user) => ctx.userId contains user.id
  }

  private def isForMe(challenge: ChallengeModel)(implicit ctx: Context) =
    challenge.destUserId.fold(true)(ctx.userId.contains)

  def accept(id: String) = Open { implicit ctx =>
    OptionFuResult(env.api byId id) { c =>
      isForMe(c) ?? env.api.accept(c, ctx.me).flatMap {
        case Some(pov) => negotiate(
          html = Redirect(routes.Round.watcher(pov.gameId, "white")).fuccess,
          api = apiVersion => Env.api.roundApi.player(pov, apiVersion) map { Ok(_) }
        ) flatMap withChallengeAnonCookie(ctx.isAnon, c, false)
        case None => negotiate(
          html = Redirect(routes.Round.watcher(c.id, "white")).fuccess,
          api = _ => notFoundJson("Someone else accepted the challenge")
        )
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
            httpOnly = false.some
          )
        }
      }
    } map { cookieOption =>
      cookieOption.fold(res) { res.withCookies(_) }
    }

  def decline(id: String) = Auth { implicit ctx => me =>
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

  def toFriend(id: String) = AuthBody { implicit ctx => me =>
    import play.api.data._
    import play.api.data.Forms._
    implicit def req = ctx.body
    OptionFuResult(env.api byId id) { c =>
      if (isMine(c)) Form(single(
        "username" -> nonEmptyText
      )).bindFromRequest.fold(
        err => funit,
        username => UserRepo named username flatMap {
          case None => Redirect(routes.Challenge.show(c.id)).fuccess
          case Some(dest) => Env.challenge.granter(ctx.me, dest, c.perfType.some) flatMap {
            case Some(denied) => showChallenge(c, lila.challenge.ChallengeDenied.translated(denied).some)
            case None => env.api.setDestUser(c, dest) inject Redirect(routes.Challenge.show(c.id))
          }
        }
      )
      else notFound
    }
  }

  def rematchOf(gameId: String) = Auth { implicit ctx => me =>
    OptionFuResult(GameRepo game gameId) { g =>
      Pov.opponentOfUserId(g, me.id).flatMap(_.userId) ?? UserRepo.byId flatMap {
        _ ?? { opponent =>
          env.granter(me.some, opponent, g.perfType) flatMap {
            case Some(d) => BadRequest(jsonError {
              lila.challenge.ChallengeDenied translated d
            }).fuccess
            case _ => env.api.rematchOf(g, me) map {
              _.fold(Ok, BadRequest(jsonError("Sorry, couldn't create the rematch.")))
            }
          }
        }
      }
    }
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    env.api byId id flatMap {
      _ ?? { c =>
        getSocketUid("sri") ?? { uid =>
          env.socketHandler.join(id, uid, ctx.userId, isMine(c))
        }
      }
    }
  }
}
