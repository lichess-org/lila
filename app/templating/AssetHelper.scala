package lila.app
package templating

import play.api.mvc.RequestHeader
import play.api.libs.json.{ Json, JsValue }

import lila.app.ui.ScalatagsTemplate.*
import lila.common.AssetVersion
import lila.common.String.html.safeJsonValue

trait AssetHelper extends HasEnv:
  self: I18nHelper with SecurityHelper =>

  private lazy val netDomain      = env.net.domain
  private lazy val assetDomain    = env.net.assetDomain
  private lazy val assetBaseUrl   = env.net.assetBaseUrl
  private lazy val socketDomains  = env.net.socketDomains
  private lazy val minifiedAssets = env.net.minifiedAssets
  lazy val vapidPublicKey         = env.push.vapidPublicKey

  lazy val picfitUrl = env.memo.picfitUrl

  lazy val sameAssetDomain = netDomain == assetDomain

  def assetVersion = AssetVersion.current

  def assetUrl(path: String): String       = s"$assetBaseUrl/assets/_$assetVersion/$path"
  def staticAssetUrl(path: String): String = s"$assetBaseUrl/assets/$path"

  def cdnUrl(path: String) = s"$assetBaseUrl$path"

  def cssTag(name: String)(using ctx: Context): Frag =
    cssTagWithDirAndTheme(name, isRTL, ctx.pref.currentBg)

  def cssTagWithDirAndTheme(name: String, isRTL: Boolean, theme: String): Frag =
    if theme == "system" then
      frag(
        cssTagWithDirAndSimpleTheme(name, isRTL, "light")(media := "(prefers-color-scheme: light)"),
        cssTagWithDirAndSimpleTheme(name, isRTL, "dark")(media  := "(prefers-color-scheme: dark)")
      )
    else cssTagWithDirAndSimpleTheme(name, isRTL, theme)

  private def cssTagWithDirAndSimpleTheme(name: String, isRTL: Boolean, theme: String): Tag =
    cssAt(
      s"css/$name.${if isRTL then "rtl" else "ltr"}.$theme.${if minifiedAssets then "min" else "dev"}.css"
    )

  def cssTagNoTheme(name: String): Frag =
    cssAt(s"css/$name.${if minifiedAssets then "min" else "dev"}.css")

  private def cssAt(path: String): Tag =
    link(href := assetUrl(path), rel := "stylesheet")

  val systemThemePolyfillJs = """
if (window.matchMedia('(prefers-color-scheme: dark)').media === 'not all')
    document.querySelectorAll('[media="(prefers-color-scheme: dark)"]').forEach(e=>e.media='')
"""

  // load iife scripts in <head> and defer
  def iifeModule(path: String): Frag = script(deferAttr, src := assetUrl(path))

  // jsModule is esm, no defer needed
  def jsModule(name: String): Frag =
    script(tpe := "module", src := assetUrl(s"compiled/$name${minifiedAssets so ".min"}.js"))
  def jsModuleInit(name: String)(using PageContext) =
    frag(jsModule(name), embedJsUnsafeLoadThen(s"lichess.loadEsm('$name')"))
  def jsModuleInit(name: String, text: String)(using PageContext) =
    frag(jsModule(name), embedJsUnsafeLoadThen(s"lichess.loadEsm('$name',{init:$text})"))
  def jsModuleInit(name: String, json: JsValue)(using PageContext): Frag =
    jsModuleInit(name, safeJsonValue(json))
  def jsModuleInit(name: String, text: String, nonce: lila.api.Nonce) =
    frag(jsModule(name), embedJsUnsafeLoadThen(s"lichess.loadEsm('$name',{init:$text})", nonce))
  def jsModuleInit(name: String, json: JsValue, nonce: lila.api.Nonce) = frag(
    jsModule(name),
    embedJsUnsafeLoadThen(s"lichess.loadEsm('$name',{init:${safeJsonValue(json)}})", nonce)
  )
  def analyseInit(mode: String, json: JsValue)(using ctx: PageContext) =
    jsModuleInit("analysisBoard", Json.obj("mode" -> mode, "cfg" -> json))

  def analyseNvuiTag(using ctx: PageContext)    = ctx.blind option jsModule("analysisBoard.nvui")
  def puzzleNvuiTag(using ctx: PageContext)     = ctx.blind option jsModule("puzzle.nvui")
  def roundNvuiTag(using ctx: PageContext)      = ctx.blind option jsModule("round.nvui")
  def infiniteScrollTag(using ctx: PageContext) = jsModuleInit("infiniteScroll", "'.infinite-scroll'")
  def captchaTag                                = jsModule("captcha")
  def cashTag                                   = iifeModule("javascripts/vendor/cash.min.js")
  def fingerprintTag                            = iifeModule("javascripts/fipr.js")
  def highchartsLatestTag                       = iifeModule("npm/highcharts-4.2.5/highcharts.js")
  def highchartsMoreTag                         = iifeModule("npm/highcharts-4.2.5/highcharts-more.js")
  def chessgroundTag = script(tpe := "module", src := assetUrl("npm/chessground.min.js"))

  def prismicJs(using PageContext): Frag =
    raw:
      isGranted(_.Prismic).so:
        embedJsUnsafe("""window.prismic={endpoint:'https://lichess.prismic.io/api/v2'}""").render ++
          """<script src="//static.cdn.prismic.io/prismic.min.js"></script>"""

  def basicCsp(using ctx: Context): ContentSecurityPolicy =
    val sockets = socketDomains map { x => s"wss://$x${!ctx.req.secure so s" ws://$x"}" }
    // include both ws and wss when insecure because requests may come through a secure proxy
    val localDev = !ctx.req.secure so List("http://127.0.0.1:3000")
    ContentSecurityPolicy(
      defaultSrc = List("'self'", assetDomain.value),
      connectSrc =
        "'self'" :: "data:" :: assetDomain.value :: sockets ::: env.explorerEndpoint :: env.tablebaseEndpoint :: localDev,
      styleSrc = List("'self'", "'unsafe-inline'", assetDomain.value),
      frameSrc = List("'self'", assetDomain.value, "www.youtube.com", "player.twitch.tv"),
      workerSrc = List("'self'", assetDomain.value, "blob:"),
      imgSrc = List("data:", "*"),
      scriptSrc = List("'self'", assetDomain.value),
      fontSrc = List("'self'", assetDomain.value),
      baseUri = List("'none'")
    )

  def defaultCsp(using ctx: PageContext): ContentSecurityPolicy =
    ctx.nonce.foldLeft(basicCsp)(_ withNonce _)

  def analysisCsp(using PageContext): ContentSecurityPolicy =
    defaultCsp.withWebAssembly.withExternalEngine(env.externalEngineEndpoint)

  def embedJsUnsafe(js: String)(using ctx: PageContext): Frag = raw:
    val nonce = ctx.nonce.so: nonce =>
      s""" nonce="$nonce""""
    s"""<script$nonce>$js</script>"""

  def embedJsUnsafe(js: String, nonce: lila.api.Nonce): Frag = raw:
    s"""<script nonce="$nonce">$js</script>"""

  def embedJsUnsafeLoadThen(js: String)(using PageContext): Frag =
    embedJsUnsafe(s"""lichess.load.then(()=>{$js})""")

  def embedJsUnsafeLoadThen(js: String, nonce: lila.api.Nonce): Frag =
    embedJsUnsafe(s"""lichess.load.then(()=>{$js})""", nonce)
