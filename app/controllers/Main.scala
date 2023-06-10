package controllers

import akka.pattern.ask
import play.api.data.*, Forms.*
import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.api.WebContext
import lila.app.{ *, given }
import lila.hub.actorApi.captcha.ValidCaptcha

final class Main(
    env: Env,
    prismicC: Prismic,
    assetsC: ExternalAssets
) extends LilaController(env):

  private lazy val blindForm = Form(
    tuple(
      "enable"   -> nonEmptyText,
      "redirect" -> nonEmptyText
    )
  )

  def toggleBlindMode = OpenBody:
    fuccess:
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

  def handlerNotFound(req: RequestHeader) = reqToCtx(req) map renderNotFound

  def captchaCheck(id: GameId) = Open:
    import makeTimeout.large
    env.hub.captcher.actor ? ValidCaptcha(id, ~get("solution")) map { case valid: Boolean =>
      Ok(if (valid) 1 else 0)
    }

  def webmasters = Open:
    pageHit
    fuccess:
      html.site.page.webmasters

  def lag = Open:
    pageHit
    fuccess:
      html.site.lag()

  def mobile     = Open(serveMobile)
  def mobileLang = LangPage(routes.Main.mobile)(serveMobile)

  private def serveMobile(using WebContext) =
    pageHit
    OptionOk(prismicC getBookmark "mobile-apk"): (doc, resolver) =>
      html.mobile(doc, resolver)

  def dailyPuzzleSlackApp = Open:
    pageHit
    fuccess:
      html.site.dailyPuzzleSlackApp()

  def jslog(id: GameFullId) = Open:
    env.round.selfReport(
      userId = ctx.userId,
      ip = ctx.ip,
      fullId = id,
      name = get("n", ctx.req) | "?"
    )
    NoContent.toFuccess

  val robots = Anon:
    fuccess:
      Ok:
        if env.net.crawlable && req.domain == env.net.domain.value && env.net.isProd
        then lila.api.StaticContent.robotsTxt
        else "User-agent: *\nDisallow: /"

  def manifest = Anon:
    fuccess:
      JsonOk:
        lila.api.StaticContent.manifest(env.net)

  def getFishnet = Open:
    pageHit
    Ok(html.site.bits.getFishnet()).toFuccess

  def costs = Anon:
    pageHit(req)
    fuccess:
      Redirect:
        "https://docs.google.com/spreadsheets/d/1Si3PMUJGR9KrpE5lngSkHLJKJkb0ZuI4/preview"

  def verifyTitle = Anon:
    pageHit(req)
    fuccess:
      Redirect:
        "https://docs.google.com/forms/d/e/1FAIpQLSelXSHdiFw_PmZetxY8AaIJSM-Ahb5QnJcfQMDaiPJSf24lDQ/viewform"

  def contact = Open:
    pageHit
    Ok(html.site.contact()).toFuccess

  def faq = Open:
    pageHit
    Ok(html.site.faq()).toFuccess

  def temporarilyDisabled = Open:
    pageHit
    NotImplemented(html.site.message.temporarilyDisabled).toFuccess

  def keyboardMoveHelp = Open:
    Ok(html.site.helpModal.keyboardMove).toFuccess

  def voiceHelp(module: String) = Open:
    module match
      case "move"   => Ok(html.site.helpModal.voiceMove).toFuccess
      case "coords" => Ok(html.site.helpModal.voiceCoords).toFuccess
      case _        => NotFound(s"Unknown voice help module: $module").toFuccess

  def movedPermanently(to: String) = Anon:
    MovedPermanently(to).toFuccess

  def instantChess = Open:
    pageHit
    if ctx.isAuth
    then fuccess(Redirect(routes.Lobby.home))
    else
      fuccess:
        Redirect(s"${routes.Lobby.home}#pool/10+0").withCookies(
          env.lilaCookie.withSession(remember = true) { s =>
            s + ("theme" -> "ic") + ("pieceSet" -> "icpieces")
          }
        )

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
        case 50   => routes.Page.help.url
        case 46   => s"$faq#name"
        case 122  => s"$faq#marks"
        case _    => faq
    .toFuccess

  def devAsset(v: String, path: String, file: String) = assetsC.at(path, file)

  private val ImageUploadRateLimitPerIp = lila.memo.RateLimit.composite[lila.common.IpAddress](
    key = "image.upload.ip"
  )(
    ("fast", 10, 2.minutes),
    ("slow", 60, 1.day)
  )

  def uploadImage(rel: String) = AuthBody(parse.multipartFormData) { ctx ?=> me =>
    ctx.body.body.file("image") match
      case Some(image) =>
        env.memo.picfitApi.bodyImage
          .upload(rel = rel, image = image, me = me.id, ip = ctx.ip)
          .map(url => JsonOk(Json.obj("imageUrl" -> url)))
      case None => fuccess(JsonBadRequest(jsonError("Image content only")))
  }
