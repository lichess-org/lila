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
import lila.game.{ GameRepo, AnonCookie }
import views.html

object Challenge extends LilaController {

  private def env = Env.challenge

  private val PostRateLimit = new lila.memo.RateLimit(5, 1 minute)

  def all = Auth { implicit ctx =>
    me =>
      env.api allFor me.id map { all =>
        Ok(env.jsonView(all)) as JSON
      }
  }

  def show(id: String) = Open { implicit ctx =>
    showId(id)
  }

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
      challengeAnonOwnerCookie(c) flatMap { cookieOption =>
        negotiate(
          html = fuccess {
            Ok(mine.fold(html.challenge.mine.apply _, html.challenge.theirs.apply _)(c, json))
          },
          api = _ => Ok(json).fuccess
        ) map { res =>
            cookieOption.fold(res) { res.withCookies(_) }
          }
      }
    }

  private def challengeAnonOwnerCookie(c: ChallengeModel)(implicit ctx: Context): Fu[Option[Cookie]] =
    (c.accepted && c.challengerIsAnon) ?? GameRepo.game(c.id).map {
      _ map { game =>
        implicit val req = ctx.req
        LilaCookie.cookie(
          AnonCookie.name,
          game.player(c.finalColor).id,
          maxAge = AnonCookie.maxAge.some,
          httpOnly = false.some)
      }
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
          html = Setup.redirectPov(pov).fuccess,
          api = apiVersion =>
            Env.api.roundApi.player(pov, apiVersion) map { Ok(_) })
        case None => negotiate(
          html = Redirect(routes.Round.watcher(c.id, "white")).fuccess,
          api = _ => notFoundJson("Someone else accepted the challenge"))
      }
    }
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
        env.api.rematchOf(g, me) map {
          _.fold(Ok, BadRequest)
        }
      }
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    env.api byId id flatMap {
      _ ?? { c =>
        get("sri") ?? { uid =>
          env.socketHandler.join(id, uid, ctx.userId, isMine(c))
        }
      }
    }
  }
}
