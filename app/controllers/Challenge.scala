package controllers

import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import play.api.mvc.{ Result, Results, Call, RequestHeader, Accepting }
import scala.concurrent.duration._

import lila.api.{ Context, BodyContext }
import lila.app._
import lila.challenge.{ Challenge => ChallengeModel }
import lila.common.{ HTTPRequest, LilaCookie }
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
      val json = env.jsonView.show(c, version)
      negotiate(
        html = Ok(isMine(c).fold(
          html.challenge.mine.apply _,
          html.challenge.theirs.apply _
        )(c, json)).fuccess,
        api = _ => Ok(json).fuccess)
    }

  private def isMine(challenge: ChallengeModel)(implicit ctx: Context) = challenge.challenger match {
    case Left(anon)  => HTTPRequest sid ctx.req contains anon.secret
    case Right(user) => ctx.userId contains user.id
  }

  private def isForMe(challenge: ChallengeModel)(implicit ctx: Context) =
    challenge.destUserId.fold(true)(ctx.userId.contains)

  def accept(id: String) = Open { implicit ctx =>
    OptionFuResult(env.api byId id) { c =>
      if (isForMe(c)) env.api.accept(c, ctx.me) map { game =>
        Redirect(routes.Round.watcher(game.id, "white"))
      }
      else notFound
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
    OptionFuResult(env.api byId id) { challenge =>
      if (isMine(challenge)) env.api cancel challenge inject Redirect(routes.Lobby.home)
      else notFound
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
