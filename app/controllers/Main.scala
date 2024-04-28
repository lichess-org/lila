package controllers

import akka.pattern.ask
import play.api.data.*
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.id.GameFullId

import Forms.*

final class Main(
    env: Env,
    assetsC: ExternalAssets
) extends LilaController(env):

  private lazy val blindForm = Form:
    tuple(
      "enable"   -> nonEmptyText,
      "redirect" -> nonEmptyText
    )

  def toggleBlindMode = OpenBody:
    blindForm
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
    Ok.page(views.site.lag())

  def mobile     = Open(serveMobile)
  def mobileLang = LangPage(routes.Main.mobile)(serveMobile)

  def redirectToAppStore = Anon:
    pageHit
    Redirect:
      if HTTPRequest.isAndroid(req)
      then "https://play.google.com/store/apps/details?id=org.lichess.mobileapp"
      else "https://apps.apple.com/us/app/lichess-online-chess/id968371784"

  private def serveMobile(using Context) =
    pageHit
    FoundPage(env.api.cmsRenderKey("mobile-apk"))(views.mobile.apply)

  def dailyPuzzleSlackApp = Open:
    Ok.page(views.site.dailyPuzzleSlackApp())

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
      then lila.web.StaticContent.robotsTxt
      else "User-agent: *\nDisallow: /"

  def manifest = Anon:
    JsonOk:
      lila.web.StaticContent.manifest(env.net)

  def getFishnet = Open:
    pageHit
    Ok.page(views.site.bits.getFishnet())

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
    Ok.page(views.site.page.faq)

  def temporarilyDisabled(path: String) = Open:
    pageHit
    NotImplemented.page(views.site.message.temporarilyDisabled)

  def keyboardMoveHelp = Open:
    Ok.page(lila.web.views.help.keyboardMove)

  def voiceHelp(module: String) = Open:
    module match
      case "move" => Ok.page(lila.web.views.help.voiceMove)
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

  def legacyQaQuestion(id: Int, slug: String) = Open:
    MovedPermanently:
      val faq = routes.Main.faq.url
      id match
        case 103  => s"$faq#acpl"
        case 258  => s"$faq#marks"
        case 13   => s"$faq#titles"
        case 87   => routes.User.ratingDistribution("blitz").url
        case 110  => s"$faq#name"
        case 29   => s"$faq#titles"
        case 4811 => s"$faq#lm"
        case 216  => routes.Main.mobile.url
        case 340  => s"$faq#trophies"
        case 6    => s"$faq#ratings"
        case 207  => s"$faq#hide-ratings"
        case 547  => s"$faq#leaving"
        case 259  => s"$faq#trophies"
        case 342  => s"$faq#provisional"
        case 50   => routes.Cms.help.url
        case 46   => s"$faq#name"
        case 122  => s"$faq#marks"
        case _    => faq

  def devAsset(v: String, path: String, file: String) = assetsC.at(path, file)

  private val externalMonitorOnce = scalalib.cache.OnceEvery.hashCode[String](10.minutes)
  def externalLink(tag: String, url: String) = Anon:
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
