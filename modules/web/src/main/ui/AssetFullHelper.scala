package lila.web
package ui
import play.api.libs.json.{ JsValue, Json, Writes }

import lila.ui.ScalatagsTemplate.*
import lila.core.data.SafeJsonStr
import lila.common.String.html.safeJsonValue
import lila.web.ui.*
import lila.core.config.NetConfig
import lila.ui.{ Nonce, Optionce, WithNonce, ContentSecurityPolicy, EsmList, Context }

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

  def cssTag(key: String): Frag =
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
  def jsDeps(keys: List[String]): Frag = frag:
    manifest.deps(keys).map { dep =>
      script(tpe := "module", src := staticAssetUrl(s"compiled/$dep"))
    }
  def jsModuleInit(key: String): EsmInit =
    EsmInit(key, embedJsUnsafeLoadThen(s"$load('${jsName(key)}')"))
  def jsModuleInit(key: String, json: SafeJsonStr): EsmInit =
    EsmInit(key, embedJsUnsafeLoadThen(s"$load('${jsName(key)}',{init:$json})"))
  def jsModuleInit[A: Writes](key: String, value: A): EsmInit =
    jsModuleInit(key, safeJsonValue(Json.toJson(value)))
  def jsPageModule(key: String): EsmInit =
    EsmInit(key, embedJsUnsafeLoadThen(s"site.asset.loadPageEsm('${jsName(key)}')"))

  def analyseNvuiTag(using ctx: Context) = ctx.blind.option(EsmInit("analyse.nvui"))
  def puzzleNvuiTag(using ctx: Context)  = ctx.blind.option(EsmInit("puzzle.nvui"))
  def roundNvuiTag(using ctx: Context)   = ctx.blind.option(EsmInit("round.nvui"))
  val infiniteScrollEsmInit: EsmInit     = jsModuleInit("bits.infiniteScroll")
  val captchaEsmInit: EsmInit            = EsmInit("bits.captcha")
  val cashTag: Frag                      = iifeModule("javascripts/vendor/cash.min.js")
  val fingerprintTag: Frag               = iifeModule("javascripts/fipr.js")
  val chessgroundTag: Frag               = script(tpe := "module", src := assetUrl("npm/chessground.min.js"))

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
