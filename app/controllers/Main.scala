package controllers

import akka.pattern.ask
import play.api.data.*, Forms.*
import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.app.{ *, given }
import lila.hub.actorApi.captcha.ValidCaptcha
import lila.common.HTTPRequest

final class Main(
    env: Env,
    prismicC: Prismic,
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
            lila.api.ApiConfig.blindCookie.make(env.lilaCookie)(enable != "0")
      )

  def handlerNotFound(using RequestHeader) =
    makeContext.flatMap:
      keyPages.notFound(using _)

  def captchaCheck(id: GameId) = Open:
    import makeTimeout.long
    env.hub.captcher.actor ? ValidCaptcha(id, ~get("solution")) map { case valid: Boolean =>
      Ok(if valid then 1 else 0)
    }

  def webmasters = Open:
    Ok.page(html.site.page.webmasters)

  def lag = Open:
    Ok.page(html.site.lag())

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
    FoundPage(prismicC getBookmark "mobile-apk"): (doc, resolver) =>
      html.mobile(doc, resolver)

  def dailyPuzzleSlackApp = Open:
    Ok.page(html.site.dailyPuzzleSlackApp())

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
      then lila.api.StaticContent.robotsTxt
      else "User-agent: *\nDisallow: /"

  def manifest = Anon:
    JsonOk:
      lila.api.StaticContent.manifest(env.net)

  def getFishnet = Open:
    pageHit
    Ok.page(html.site.bits.getFishnet())

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
    Ok.page(html.site.contact())

  def faq = Open:
    pageHit
    Ok.page(html.site.faq())

  def temporarilyDisabled = Open:
    pageHit
    NotImplemented.page(html.site.message.temporarilyDisabled)

  def analyseVariationArrowHelp = Open:
    Ok.page(html.site.help.analyseVariationArrow)

  def keyboardMoveHelp = Open:
    Ok.page(html.site.help.keyboardMove)

  def voiceHelp(module: String) = Open:
    module match
      case "move"   => Ok.page(html.site.help.voiceMove)
      case "coords" => Ok.page(html.site.help.voiceCoords)
      case _        => NotFound(s"Unknown voice help module: $module")

  def movedPermanently(to: String) = Anon:
    MovedPermanently(to)

  def instantChess = Open:
    pageHit
    if ctx.isAuth then Redirect(routes.Lobby.home)
    else
      Redirect(s"${routes.Lobby.home}#pool/10+0").withCookies:
        env.lilaCookie.withSession(remember = true): s =>
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
        case 50   => routes.ContentPage.help.url
        case 46   => s"$faq#name"
        case 122  => s"$faq#marks"
        case _    => faq

  def devAsset(v: String, path: String, file: String) = assetsC.at(path, file)

  private val ImageUploadRateLimitPerIp = lila.memo.RateLimit.composite[lila.common.IpAddress](
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
