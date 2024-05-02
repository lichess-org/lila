package lila.web
package ui
import play.api.libs.json.{ JsValue, Json, Writes }

import lila.ui.ScalatagsTemplate.*
import lila.core.data.SafeJsonStr
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

  export lila.common.String.html.safeJsonValue

  def jsName(key: String): String = manifest.js(key).fold(key)(_.name)

  private lazy val socketDomains = netConfig.socketDomains ::: netConfig.socketAlts
  // lazy val vapidPublicKey         = env.push.vapidPublicKey

  lazy val sameAssetDomain = netConfig.domain == netConfig.assetDomain

  lazy val siteName: String =
    if netConfig.siteName == "localhost:9663" then "lichess.dev"
    else netConfig.siteName

  def assetVersion = lila.core.net.AssetVersion.current

  def assetUrl(path: String): String = s"$assetBaseUrl/assets/_$assetVersion/$path"

  def cssTag(key: String): Frag =
    link(
      cls  := s"css-$key",
      href := staticAssetUrl(s"css/${manifest.css(key).getOrElse(key)}"),
      rel  := "stylesheet"
    )

  def jsonScript(json: JsValue | SafeJsonStr) =
    script(tpe := "application/json", st.id := "page-init-data"):
      raw:
        json match
          case json: JsValue => safeJsonValue(json).value
          case json          => json.toString

  def jsDeps(keys: List[String]): Frag = frag:
    manifest.deps(keys).map { dep =>
      script(tpe := "module", src := staticAssetUrl(s"compiled/$dep"))
    }
  def roundNvuiTag(using ctx: Context)    = ctx.blind.option(EsmInit("round.nvui"))
  lazy val infiniteScrollEsmInit: EsmInit = jsModuleInit("bits.infiniteScroll")
  lazy val captchaEsmInit: EsmInit        = EsmInit("bits.captcha")
  lazy val cashTag: Frag                  = iifeModule("javascripts/vendor/cash.min.js")
  lazy val chessgroundTag: Frag           = script(tpe := "module", src := assetUrl("npm/chessground.min.js"))

  def basicCsp(using ctx: Context): ContentSecurityPolicy =
    val sockets = socketDomains.map { x => s"wss://$x${(!ctx.req.secure).so(s" ws://$x")}" }
    // include both ws and wss when insecure because requests may come through a secure proxy
    val localDev = (!ctx.req.secure).so(List("http://127.0.0.1:3000"))
    lila.web.ContentSecurityPolicy.basic(
      netConfig.assetDomain,
      netConfig.assetDomain.value :: sockets ::: explorerEndpoint :: tablebaseEndpoint :: localDev
    )

  def defaultCsp(using nonce: Optionce)(using Context): ContentSecurityPolicy =
    nonce.foldLeft(basicCsp)(_.withNonce(_))
