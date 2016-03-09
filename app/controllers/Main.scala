package controllers

import akka.pattern.ask
import play.api.data._, Forms._
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._, Results._
import kamon.Kamon

import lila.app._
import lila.common.HTTPRequest
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
              Env.api.Accessibility.blindCookieName,
              if (enable == "0") "" else Env.api.Accessibility.hash,
              maxAge = Env.api.Accessibility.blindCookieMaxAge.some,
              httpOnly = true.some)
        })
    }
  }

  def websocket = SocketOption { implicit ctx =>
    get("sri") ?? { uid =>
      Env.site.socketHandler(uid, ctx.userId, get("flag")) map some
    }
  }

  def captchaCheck(id: String) = Open { implicit ctx =>
    Env.hub.actor.captcher ? ValidCaptcha(id, ~get("solution")) map {
      case valid: Boolean => Ok(valid fold (1, 0))
    }
  }

  def embed = Action { req =>
    Ok {
      s"""document.write("<iframe src='${Env.api.Net.BaseUrl}?embed=" + document.domain + "' class='lichess-iframe' allowtransparency='true' frameBorder='0' style='width: ${getInt("w", req) | 820}px; height: ${getInt("h", req) | 650}px;' title='Lichess free online chess'></iframe>");"""
    } as JAVASCRIPT withHeaders (CACHE_CONTROL -> "max-age=86400")
  }

  def developers = Open { implicit ctx =>
    fuccess {
      html.site.developers()
    }
  }

  def themepicker = Open { implicit ctx =>
    fuccess {
      html.base.themepicker()
    }
  }

  def lag = Open { implicit ctx =>
    fuccess {
      html.site.lag()
    }
  }

  def mobile = Open { implicit ctx =>
    OptionOk(Prismic getBookmark "mobile-apk") {
      case (doc, resolver) => html.mobile.home(doc, resolver)
    }
  }

  def mobileRegister(platform: String, deviceId: String) = Auth { implicit ctx =>
    me =>
      Env.push.registerDevice(me, platform, deviceId)
  }

  def mobileUnregister = Auth { implicit ctx =>
    me =>
      Env.push.unregisterDevices(me)
  }

  def jslog(id: String) = Open { ctx =>
    val referer = HTTPRequest.referer(ctx.req)
    loginfo(s"[jslog] ${ctx.req.remoteAddress} ${ctx.userId} $referer")
    Kamon.metrics.counter("cheat.css-bot").increment()
    ctx.userId.?? {
      Env.report.api.autoBotReport(_, referer)
    }
    lila.game.GameRepo pov id map {
      _ ?? lila.game.GameRepo.setBorderAlert
    } inject Ok
  }

  def notFound(req: RequestHeader): Fu[Result] =
    reqToCtx(req) map { implicit ctx =>
      Kamon.metrics.counter("http.response.404").increment()
      NotFound(html.base.notFound())
    }
}
