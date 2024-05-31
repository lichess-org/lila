package controllers

import akka.pattern.ask
import play.api.data._, Forms._
import play.api.libs.json._
import play.api.mvc._
import scala.annotation.nowarn

import lila.app._
import lila.common.HTTPRequest
import lila.hub.actorApi.captcha.ValidCaptcha
import makeTimeout.large
import views._

final class Main(
    env: Env,
    assetsC: ExternalAssets
) extends LilaController(env) {

  private lazy val blindForm = Form(
    tuple(
      "enable"   -> nonEmptyText,
      "redirect" -> nonEmptyText
    )
  )

  def toggleBlindMode =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      fuccess {
        blindForm
          .bindFromRequest()
          .fold(
            _ => BadRequest,
            { case (enable, redirect) =>
              Redirect(redirect) withCookies env.lilaCookie.cookie(
                env.api.config.accessibility.blindCookieName,
                if (enable == "0") "" else env.api.config.accessibility.hash,
                maxAge = env.api.config.accessibility.blindCookieMaxAge.toSeconds.toInt.some,
                httpOnly = true.some
              )
            }
          )
      }
    }

  def handlerNotFound(req: RequestHeader) = reqToCtx(req) map renderNotFound

  def captchaCheck(id: String) =
    Open { implicit ctx =>
      env.hub.captcher.actor ? ValidCaptcha(id, ~get("solution")) map { case valid: Boolean =>
        Ok(if (valid) 1 else 0)
      }
    }

  def webmasters =
    Open { implicit ctx =>
      pageHit
      fuccess {
        html.site.help.webmasters()
      }
    }

  def lag =
    Open { implicit ctx =>
      pageHit
      fuccess {
        html.site.lag()
      }
    }

  def jslog(id: String) =
    Open { ctx =>
      env.round.selfReport(
        userId = ctx.userId,
        ip = HTTPRequest lastRemoteAddress ctx.req,
        fullId = lila.game.Game.FullId(id),
        name = get("n", ctx.req) | "?"
      )
      NoContent.fuccess
    }

  /** Event monitoring endpoint
    */
  def jsmon(event: String) =
    Action {
      lila.mon.http.jsmon(event).increment()
      NoContent
    }

  def image(id: String, @nowarn("cat=unused") hash: String, @nowarn("cat=unused") name: String) =
    Action.async {
      env.imageRepo
        .fetch(id)
        .map {
          case None => NotFound
          case Some(image) =>
            lila.mon.http.imageBytes.record(image.size.toLong)
            Ok(image.data).withHeaders(
              CONTENT_DISPOSITION -> image.name
            ) as image.contentType.getOrElse("image/jpeg")
        }
    }

  val robots = Action { req =>
    Ok {
      if (env.net.crawlable && req.domain == env.net.domain.value) """User-agent: *
Allow: /
Disallow: /game/export/
Disallow: /games/export/
Disallow: /api/
Allow: /game/export/gif/thumbnail/

User-agent: Twitterbot
Allow: /
"""
      else "User-agent: *\nDisallow: /"
    }
  }

  def manifest =
    Action {
      Ok {
        Json.obj(
          "name"             -> env.net.domain.value,
          "short_name"       -> "Lishogi",
          "start_url"        -> "/",
          "display"          -> "standalone",
          "background_color" -> "#161512",
          "theme_color"      -> "#161512",
          "description"      -> "The (really) free, no-ads, open source shogi server.",
          "icons" -> List(32, 64, 128, 192, 256, 512, 1024).map { size =>
            Json.obj(
              "src"   -> s"//${env.net.assetDomain.value}/assets/logo/lishogi-favicon-$size.png",
              "sizes" -> s"${size}x${size}",
              "type"  -> "image/png"
            )
          }
        )
      } as JSON withHeaders (CACHE_CONTROL -> "max-age=1209600")
    }

  def contact =
    Open { implicit ctx =>
      pageHit
      Ok(html.site.contact()).fuccess
    }

  def faq =
    Open { implicit ctx =>
      pageHit
      Ok(html.site.faq()).fuccess
    }

  def movedPermanently(to: String) =
    Action {
      MovedPermanently(to)
    }

  def devAsset(@nowarn("cat=unused") v: String, path: String, file: String) = assetsC.at(path, file)
}
