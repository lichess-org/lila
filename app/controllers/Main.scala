package controllers

import play.api.data.*
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.id.GameFullId

import lila.core.net.Bearer
import lila.web.{ WebForms, StaticContent }

final class Main(
    env: Env,
    assetsC: ExternalAssets
) extends LilaController(env):

  def toggleBlindMode = OpenBody:
    WebForms.blind
      .bindFromRequest()
      .fold(
        _ => BadRequest,
        (enable, redirect) =>
          Redirect(redirect).withCookies:
            lila.web.WebConfig.blindCookie.make(env.security.lilaCookie)(enable != "0")
      )

  def handlerNotFound(using RequestHeader) =
    makeContext.flatMap:
      keyPages.notFound(using _)

  def captchaCheck(id: GameId) = Open:
    env.game.captcha.validate(id, ~get("solution")).map { valid =>
      Ok(if valid then 1 else 0)
    }

  def webmasters = Open:
    Ok.page(views.site.page.webmasters)

  def lag = Open:
    Ok.page(views.site.ui.lag)

  def mobile     = Open(serveMobile)
  def mobileLang = LangPage(routes.Main.mobile)(serveMobile)

  def redirectToAppStore = Anon:
    pageHit
    Redirect(StaticContent.appStoreUrl)

  private def serveMobile(using Context) =
    pageHit
    FoundPage(env.api.cmsRenderKey("mobile-apk"))(views.mobile)

  def dailyPuzzleSlackApp = Open:
    Ok.page(views.site.ui.dailyPuzzleSlackApp)

  def jslog(id: GameFullId) = Open:
    env.round.selfReport(
      userId = ctx.userId,
      ip = ctx.ip,
      fullId = id,
      name = get("n") | "?"
    )
    NoContent

  val robots = Anon:
    Ok:
      if env.net.crawlable && req.domain == env.net.domain.value && env.net.isProd
      then StaticContent.robotsTxt
      else "User-agent: *\nDisallow: /"

  def manifest = Anon:
    JsonOk:
      StaticContent.manifest(env.net)

  def getFishnet = Open:
    pageHit
    Ok.page(views.site.ui.getFishnet())

  def costs = Anon:
    pageHit
    Redirect:
      "https://docs.google.com/spreadsheets/d/1Si3PMUJGR9KrpE5lngSkHLJKJkb0ZuI4/preview"

  def verifyTitle = Anon:
    pageHit
    Redirect:
      "https://docs.google.com/forms/d/e/1FAIpQLSelXSHdiFw_PmZetxY8AaIJSM-Ahb5QnJcfQMDaiPJSf24lDQ/viewform"

  def contact = Open:
    pageHit
    Ok.page(views.site.page.contact)

  def faq = Open:
    pageHit
    Ok.page(views.site.page.faq.apply)

  def temporarilyDisabled(path: String) = Open:
    pageHit
    NotImplemented.page(views.site.message.temporarilyDisabled)

  def keyboardMoveHelp = Open:
    Ok(lila.ui.Snippet(lila.web.ui.help.keyboardMove))

  def voiceHelp(module: String) = Open:
    module match
      case "move" => Ok.snip(lila.web.ui.help.voiceMove)
      case _      => NotFound(s"Unknown voice module: $module")

  def movedPermanently(to: String) = Anon:
    MovedPermanently(to)

  def instantChess = Open:
    pageHit
    if ctx.isAuth then Redirect(routes.Lobby.home)
    else
      Redirect(s"${routes.Lobby.home}#pool/10+0").withCookies:
        env.security.lilaCookie.withSession(remember = true): s =>
          s + ("theme" -> "ic") + ("pieceSet" -> "icpieces")

  def legacyQaQuestion(id: Int, _slug: String) = Open:
    MovedPermanently:
      StaticContent.legacyQaQuestion(id)

  def devAsset(v: String, path: String, file: String) = assetsC.at(path, file)

  private val externalMonitorOnce = scalalib.cache.OnceEvery.hashCode[String](10.minutes)
  def externalLink(tag: String) = Anon:
    StaticContent.externalLinks
      .get(tag)
      .so: url =>
        if HTTPRequest.isCrawler(ctx.req).no && externalMonitorOnce(s"$tag/${ctx.ip}")
        then lila.mon.link.external(tag, ctx.isAuth).increment()
        Redirect(url)

  lila.memo.RateLimit.composite[lila.core.net.IpAddress](
    key = "image.upload.ip"
  )(
    ("fast", 10, 2.minutes),
    ("slow", 60, 1.day)
  )

  def uploadImage(rel: String) = AuthBody(parse.multipartFormData) { ctx ?=> me ?=>
    ctx.body.body.file("image") match
      case Some(image) =>
        env.memo.picfitApi.bodyImage
          .upload(rel = rel, image = image, me = me, ip = ctx.ip)
          .map(url => JsonOk(Json.obj("imageUrl" -> url)))
      case None => JsonBadRequest(jsonError("Image content only"))
  }

  def githubSecretScanning =
    AnonBodyOf(parse.json):
      _.asOpt[List[JsObject]]
        .map:
          _.flatMap: obj =>
            for
              token <- (obj \ "token").asOpt[String]
              url   <- (obj \ "url").asOpt[String]
            yield Bearer(token) -> url
          .toMap
        .so: tokensMap =>
          env.oAuth.tokenApi
            .secretScanning(tokensMap)
            .flatMap:
              _.traverse: (token, url) =>
                env.msg.api.systemPost(token.userId, lila.msg.MsgPreset.apiTokenRevoked(url))
            .as(NoContent)
