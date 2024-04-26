package lila.web
package ui
import play.api.libs.json.{ JsValue, Json, Writes }

import lila.ui.ScalatagsTemplate.*
import lila.core.data.SafeJsonStr
import lila.common.String.html.safeJsonValue
import lila.web.ui.*
import lila.web.Nonce
import lila.web.ContentSecurityPolicy
import lila.core.config.NetConfig
import lila.ui.{ EsmInit, EsmList, Context }

type Optionce = Option[Nonce]

trait AssetFullHelper:
  self: lila.ui.AssetHelper & lila.ui.I18nHelper =>
  def netConfig: NetConfig
  def manifest: AssetManifest
  def explorerEndpoint: String
  def tablebaseEndpoint: String
  def externalEngineEndpoint: String

  private lazy val socketDomains = netConfig.socketDomains ::: netConfig.socketAlts
  // lazy val vapidPublicKey         = env.push.vapidPublicKey

  given Conversion[EsmInit, EsmList] with
    def apply(esmInit: EsmInit): EsmList = List(Some(esmInit))
  given Conversion[Option[EsmInit], EsmList] with
    def apply(esmOption: Option[EsmInit]): EsmList = List(esmOption)

  lazy val sameAssetDomain = netConfig.domain == netConfig.assetDomain

  lazy val siteName: String =
    if netConfig.siteName == "localhost:9663" then "lichess.dev"
    else netConfig.siteName

  def assetVersion = lila.core.net.AssetVersion.current

  def assetUrl(path: String): String = s"$assetBaseUrl/assets/_$assetVersion/$path"

  def cssTag(key: String)(using ctx: Context): Frag =
    link(href := staticAssetUrl(s"css/${manifest.css(key).getOrElse(key)}"), rel := "stylesheet")

  def jsonScript(json: JsValue | SafeJsonStr) =
    script(tpe := "application/json", st.id := "page-init-data"):
      raw:
        json match
          case json: JsValue => safeJsonValue(json).value
          case json          => json.toString

  // load iife scripts in <head> and defer
  def iifeModule(path: String): Frag = script(deferAttr, src := assetUrl(path))

  private val load = "site.asset.loadEsm"

  def jsName(key: String): String =
    manifest.js(key).fold(key)(_.name)
  def jsTag(key: String): Frag =
    script(tpe := "module", src := staticAssetUrl(s"compiled/${jsName(key)}"))
  def jsDeps(keys: List[String]): Frag = frag:
    manifest.deps(keys).map { dep =>
      script(tpe := "module", src := staticAssetUrl(s"compiled/$dep"))
    }
  def jsModule(key: String): EsmInit =
    EsmInit(key, emptyFrag)
  def jsModuleInit(key: String)(using Optionce): EsmInit =
    EsmInit(key, embedJsUnsafeLoadThen(s"$load('${jsName(key)}')"))
  def jsModuleInit(key: String, json: SafeJsonStr)(using Optionce): EsmInit =
    EsmInit(key, embedJsUnsafeLoadThen(s"$load('${jsName(key)}',{init:$json})"))
  def jsModuleInit[A: Writes](key: String, value: A)(using Optionce): EsmInit =
    jsModuleInit(key, safeJsonValue(Json.toJson(value)))
  def jsModuleInit(key: String, text: SafeJsonStr, nonce: Nonce): EsmInit =
    EsmInit(key, embedJsUnsafeLoadThen(s"$load('${jsName(key)}',{init:$text})", nonce))
  def jsModuleInit(key: String, json: JsValue, nonce: Nonce): EsmInit =
    jsModuleInit(key, safeJsonValue(json), nonce)
  def jsPageModule(key: String)(using Optionce): EsmInit =
    EsmInit(key, embedJsUnsafeLoadThen(s"site.asset.loadPageEsm('${jsName(key)}')"))

  def analyseNvuiTag(using ctx: Context)(using Optionce) = ctx.blind.option(jsModule("analyse.nvui"))
  def puzzleNvuiTag(using ctx: Context)(using Optionce)  = ctx.blind.option(jsModule("puzzle.nvui"))
  def roundNvuiTag(using ctx: Context)(using Optionce)   = ctx.blind.option(jsModule("round.nvui"))
  def infiniteScrollTag(using Optionce)                  = jsModuleInit("bits.infiniteScroll")
  def captchaTag                                         = jsModule("bits.captcha")
  def cashTag                                            = iifeModule("javascripts/vendor/cash.min.js")
  def fingerprintTag                                     = iifeModule("javascripts/fipr.js")
  def chessgroundTag = script(tpe := "module", src := assetUrl("npm/chessground.min.js"))

  def basicCsp(using ctx: Context): ContentSecurityPolicy =
    val sockets = socketDomains.map { x => s"wss://$x${(!ctx.req.secure).so(s" ws://$x")}" }
    // include both ws and wss when insecure because requests may come through a secure proxy
    val localDev = (!ctx.req.secure).so(List("http://127.0.0.1:3000"))
    ContentSecurityPolicy.basic(
      netConfig.assetDomain,
      netConfig.assetDomain.value :: sockets ::: explorerEndpoint :: tablebaseEndpoint :: localDev
    )

  def defaultCsp(using nonce: Optionce)(using Context): ContentSecurityPolicy =
    nonce.foldLeft(basicCsp)(_.withNonce(_))

  def analysisCsp(using Optionce, Context): ContentSecurityPolicy =
    defaultCsp.withWebAssembly.withExternalEngine(externalEngineEndpoint)

  def embedJsUnsafe(js: String)(using nonce: Optionce): Frag = raw:
    val nonceAttr = nonce.so(n => s""" nonce="$n"""")
    s"""<script$nonceAttr>$js</script>"""

  def embedJsUnsafe(js: String, nonce: Nonce): Frag = raw:
    s"""<script nonce="$nonce">$js</script>"""

  private val onLoadFunction = "site.load.then"

  def embedJsUnsafeLoadThen(js: String)(using Optionce): Frag =
    embedJsUnsafe(s"""$onLoadFunction(()=>{$js})""")

  def embedJsUnsafeLoadThen(js: String, nonce: Nonce): Frag =
    embedJsUnsafe(s"""$onLoadFunction(()=>{$js})""", nonce)
