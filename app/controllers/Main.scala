package controllers
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.id.GameFullId
import lila.web.{ StaticContent, WebForms }

final class Main(
    env: Env,
    assetsC: ExternalAssets
) extends LilaController(env):

  def toggleBlindMode = OpenBody:
    bindForm(WebForms.blind)(
      _ => BadRequest,
      (enable, redirect) =>
        Redirect(redirect).withCookies:
          lila.web.WebConfig.blindCookie.make(env.security.lilaCookie)(enable != "0")
    )

  def handlerNotFound(msg: Option[String])(using RequestHeader) =
    makeContext.flatMap:
      keyPages.notFound(msg)(using _)

  def captchaCheck(id: GameId) = Anon:
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

  def redirectToSwag = Anon:
    Redirect(StaticContent.swagUrl(env.security.geoIP(ctx.ip).so(_.countryCode)))

  private def serveMobile(using Context) =
    pageHit
    FoundPage(env.cms.renderKey("mobile"))(views.mobile)

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

  def helpPath(path: String) = Open:
    path match
      case "keyboard-move" => Ok.snip(lila.web.ui.help.keyboardMove)
      case "voice/move"    => Ok.snip(lila.web.ui.help.voiceMove)
      case "master"        => Redirect(routes.TitleVerify.index.url)
      case _               => notFound

  def movedPermanently(to: String) = Anon:
    MovedPermanently(to)

  def instantChess = Open:
    pageHit
    if ctx.isAuth then Redirect(routes.Lobby.home)
    else
      Redirect(s"${routes.Lobby.home}#pool/10+0").withCookies:
        env.security.lilaCookie.withSession(remember = true): s =>
          s + ("theme" -> "ic") + ("pieceSet" -> "icpieces")

  def prometheusMetrics(key: String) = Anon:
    if key == env.web.config.prometheusKey
    then
      lila.web.PrometheusReporter
        .latestScrapeData()
        .fold(NotFound("No metrics found")): data =>
          lila.mon.prometheus.lines.update(data.lines.count.toDouble)
          Ok(data)
    else NotFound("Invalid prometheus key")

  def legacyQaQuestion(id: Int, _slug: String) = Anon:
    MovedPermanently:
      StaticContent.legacyQaQuestion(id)

  def devAsset(v: String, path: String, file: String) = assetsC.at(path, file)

  private val externalMonitorOnce = scalalib.cache.OnceEvery.hashCode[String](10.minutes)
  def externalLink(tag: String) = Open:
    StaticContent.externalLinks
      .get(tag)
      .so: url =>
        if HTTPRequest.isCrawler(ctx.req).no && externalMonitorOnce(s"$tag/${ctx.ip}")
        then lila.mon.link.external(tag, ctx.isAuth).increment()
        Redirect(url)

  def uploadImage(rel: String) = AuthBody(parse.multipartFormData) { ctx ?=> _ ?=>
    ctx.body.body.file("image") match
      case Some(image) =>
        limit.imageUpload(ctx.ip, rateLimited):
          env.memo.picfitApi.bodyImage
            .upload(rel, image)
            .map(url => JsonOk(Json.obj("imageUrl" -> url)))
            .recover:
              case e: Exception => JsonBadRequest(jsonError(e.getMessage))
      case None => JsonBadRequest(jsonError("Image content only"))
  }
