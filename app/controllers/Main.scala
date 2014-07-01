package controllers

import akka.pattern.ask
import play.api.data._, Forms._
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._, Results._

import lila.app._
import lila.hub.actorApi.captcha.ValidCaptcha
import makeTimeout.large
import views._

object Main extends LilaController {

  private lazy val blindForm = Form(tuple(
    "enable" -> nonEmptyText,
    "redirect" -> nonEmptyText
  ))

  def toggleBlindMode = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    fuccess {
      blindForm.bindFromRequest.fold(
        err => BadRequest, {
          case (enable, redirect) =>
            Redirect(redirect) withCookies lila.common.LilaCookie.cookie(
              Env.api.accessibilityBlindCookieName,
              enable,
              maxAge = Env.api.accessibilityBlindCookieMaxAge.some)
        })
    }
  }

  def websocket = Socket { implicit ctx =>
    get("sri") ?? { uid =>
      Env.site.socketHandler(uid, ctx.userId, get("flag"))
    }
  }

  def stream = Action.async {
    import lila.round.MoveBroadcast
    Env.round.moveBroadcast ? MoveBroadcast.GetEnumerator mapTo
      manifest[Enumerator[String]] map { e => Ok.feed(e) }
  }

  def captchaCheck(id: String) = Open { implicit ctx =>
    Env.hub.actor.captcher ? ValidCaptcha(id, ~get("solution")) map {
      case valid: Boolean => Ok(valid fold (1, 0))
    }
  }

  def embed = Action { req =>
    Ok {
      """document.write("<iframe src='%s?embed=" + document.domain + "' class='lichess-iframe' allowtransparency='true' frameBorder='0' style='width: %dpx; height: %dpx;' title='Lichess free online chess'></iframe>");"""
        .format(Env.api.Net.BaseUrl, getInt("w", req) | 820, getInt("h", req) | 650)
    } as JAVASCRIPT withHeaders (CACHE_CONTROL -> "max-age=86400")
  }

  def developers = Open { implicit ctx =>
    fuccess {
      views.html.site.developers()
    }
  }

  def irc = Open { implicit ctx =>
    ctx.me ?? Env.team.api.mine map {
      views.html.site.irc(_)
    }
  }
}
