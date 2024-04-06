package lila.app
package templating
import play.api.libs.json.{ JsValue, Json, Writes }

import lila.app.ui.ScalatagsTemplate.*
import lila.core.AssetVersion
import lila.common.String.html.safeJsonValue

trait AssetHelper extends HasEnv:
  self: I18nHelper & SecurityHelper =>

  case class PageModule(name: String, data: JsValue | SafeJsonStr)

  private lazy val netDomain      = env.net.domain
  private lazy val assetDomain    = env.net.assetDomain
  private lazy val assetBaseUrl   = env.net.assetBaseUrl
  private lazy val socketDomains  = env.net.socketDomains ::: env.net.socketAlts
  private lazy val minifiedAssets = env.net.minifiedAssets
  lazy val vapidPublicKey         = env.push.vapidPublicKey

  lazy val picfitUrl = env.memo.picfitUrl

  lazy val sameAssetDomain = netDomain == assetDomain

  def assetVersion = AssetVersion.current

  // bump flairs version if a flair is changed only (not added or removed)
  val flairVersion = "______2"

  def assetUrl(path: String): String       = s"$assetBaseUrl/assets/_$assetVersion/$path"
  def staticAssetUrl(path: String): String = s"$assetBaseUrl/assets/$path"

  def cdnUrl(path: String) = s"$assetBaseUrl$path"

  def flairSrc(flair: Flair) = staticAssetUrl(s"$flairVersion/flair/img/$flair.webp")

  def cssTag(key: String)(using ctx: Context): Frag =
    link(href := staticAssetUrl(s"css/${env.manifest.css(key).getOrElse(key)}"), rel := "stylesheet")

  def jsonScript(json: JsValue | SafeJsonStr, id: String = "page-init-data") =
    script(tpe := "application/json", st.id := id):
      raw:
        json match
          case json: JsValue => safeJsonValue(json).value
          case json          => json.toString

  // load iife scripts in <head> and defer
  def iifeModule(path: String): Frag = script(deferAttr, src := assetUrl(path))

  private val loadEsmFunction = "site.asset.loadEsm"

  def jsModuleName(key: String): String =
    env.manifest
      .js(key)
      .fold(key): asset =>
        asset.name

  def jsModuleDeps(key: String): Frag =
    env.manifest
      .js(key)
      .fold(emptyFrag): asset =>
        asset.imports.map: dep =>
          script(tpe := "module", src := staticAssetUrl(s"compiled/$dep"))

  def jsModule(key: String): Frag =
    frag(
      script(tpe := "module", src := staticAssetUrl(s"compiled/${jsModuleName(key)}")),
      jsModuleDeps(key)
    )
  def jsModuleInit(key: String)(using PageContext): Frag =
    frag(jsModule(key), embedJsUnsafeLoadThen(s"$loadEsmFunction('${jsModuleName(key)}')"))
  def jsModuleInit(key: String, json: SafeJsonStr)(using PageContext): Frag =
    frag(jsModule(key), embedJsUnsafeLoadThen(s"$loadEsmFunction('${jsModuleName(key)}',{init:$json})"))
  def jsModuleInit[A: Writes](key: String, value: A)(using PageContext): Frag =
    jsModuleInit(key, safeJsonValue(Json.toJson(value)))
  def jsModuleInit(key: String, text: SafeJsonStr, nonce: lila.api.Nonce): Frag =
    frag(
      jsModule(key),
      embedJsUnsafeLoadThen(s"$loadEsmFunction('${jsModuleName(key)}',{init:$text})", nonce)
    )
  def jsModuleInit(key: String, json: JsValue, nonce: lila.api.Nonce): Frag =
    jsModuleInit(key, safeJsonValue(json), nonce)
  def jsPageModule(key: String)(using PageContext) =
    frag(jsModule(key), embedJsUnsafeLoadThen(s"site.asset.loadPageEsm('${jsModuleName(key)}')"))

  def analyseNvuiTag(using ctx: PageContext) = ctx.blind.option(jsModule("analyse.nvui"))
  def puzzleNvuiTag(using ctx: PageContext)  = ctx.blind.option(jsModule("puzzle.nvui"))
  def roundNvuiTag(using ctx: PageContext)   = ctx.blind.option(jsModule("round.nvui"))
  def infiniteScrollTag(using PageContext)   = jsModuleInit("pagelets.infiniteScroll")
  def captchaTag                             = jsModule("pagelets.captcha")
  def cashTag                                = iifeModule("javascripts/vendor/cash.min.js")
  def fingerprintTag                         = iifeModule("javascripts/fipr.js")
  def chessgroundTag = script(tpe := "module", src := assetUrl("npm/chessground.min.js"))

  def basicCsp(using ctx: Context): ContentSecurityPolicy =
    val sockets = socketDomains.map { x => s"wss://$x${(!ctx.req.secure).so(s" ws://$x")}" }
    // include both ws and wss when insecure because requests may come through a secure proxy
    val localDev = (!ctx.req.secure).so(List("http://127.0.0.1:3000"))
    ContentSecurityPolicy(
      defaultSrc = List("'self'", assetDomain.value),
      connectSrc =
        "'self'" :: "blob:" :: "data:" :: assetDomain.value :: sockets ::: env.explorerEndpoint :: env.tablebaseEndpoint :: localDev,
      styleSrc = List("'self'", "'unsafe-inline'", assetDomain.value),
      frameSrc = List("'self'", assetDomain.value, "www.youtube.com", "player.twitch.tv"),
      workerSrc = List("'self'", assetDomain.value, "blob:"),
      imgSrc = List("'self'", "blob:", "data:", "*"),
      scriptSrc = List("'self'", assetDomain.value),
      fontSrc = List("'self'", assetDomain.value),
      baseUri = List("'none'")
    )

  def defaultCsp(using ctx: PageContext): ContentSecurityPolicy =
    ctx.nonce.foldLeft(basicCsp)(_.withNonce(_))

  def analysisCsp(using PageContext): ContentSecurityPolicy =
    defaultCsp.withWebAssembly.withExternalEngine(env.externalEngineEndpoint)

  def embedJsUnsafe(js: String)(using ctx: PageContext): Frag = raw:
    val nonce = ctx.nonce.so: nonce =>
      s""" nonce="$nonce""""
    s"""<script$nonce>$js</script>"""

  def embedJsUnsafe(js: String, nonce: lila.api.Nonce): Frag = raw:
    s"""<script nonce="$nonce">$js</script>"""

  private val onLoadFunction = "site.load.then"

  def embedJsUnsafeLoadThen(js: String)(using PageContext): Frag =
    embedJsUnsafe(s"""$onLoadFunction(()=>{$js})""")

  def embedJsUnsafeLoadThen(js: String, nonce: lila.api.Nonce): Frag =
    embedJsUnsafe(s"""$onLoadFunction(()=>{$js})""", nonce)
