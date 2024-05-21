package lila.ui

import play.api.libs.json.*

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.AssetBaseUrl
import lila.core.data.SafeJsonStr

trait AssetHelper:

  export lila.ui.EsmInit

  def manifest: AssetManifest
  def assetBaseUrl: AssetBaseUrl
  def assetUrl(path: String): String
  def infiniteScrollEsmInit: EsmInit
  def captchaEsmInit: EsmInit
  def safeJsonValue(jsValue: JsValue): SafeJsonStr

  val load = "site.asset.loadEsm"

  given Conversion[EsmInit, EsmList] with
    def apply(esmInit: EsmInit): EsmList = List(Some(esmInit))
  given Conversion[Option[EsmInit], EsmList] with
    def apply(esmOption: Option[EsmInit]): EsmList = List(esmOption)

  def jsModuleInit(key: String): EsmInit =
    EsmInit(key, embedJsUnsafeLoadThen(s"$load('$key')"))
  def jsModuleInit(key: String, json: SafeJsonStr): EsmInit =
    EsmInit(key, embedJsUnsafeLoadThen(s"$load('$key',{init:$json})"))
  def jsModuleInit[A: Writes](key: String, value: A): EsmInit =
    jsModuleInit(key, safeJsonValue(Json.toJson(value)))
  def jsPageModule(key: String): EsmInit =
    EsmInit(key, embedJsUnsafeLoadThen(s"site.asset.loadPageEsm('$key')"))

  // load iife scripts in <head> and defer
  def iifeModule(path: String): Frag = script(deferAttr, src := staticAssetUrl(path))

  def embedJsUnsafe(js: String): WithNonce[Frag] = nonce =>
    raw:
      val nonceAttr = nonce.so(n => s""" nonce="$n"""")
      s"""<script$nonceAttr>$js</script>"""

  def embedJsUnsafeLoadThen(js: String): WithNonce[Frag] = embedJsUnsafe(s"""site.load.then(()=>{$js})""")

  // bump flairs version if a flair is changed only (not added or removed)
  val flairVersion = "______2"

  def staticAssetUrl(path: String): String = s"$assetBaseUrl/assets/$path"

  def cdnUrl(path: String) = s"$assetBaseUrl$path"

  def flairSrc(flair: Flair): String = staticAssetUrl(s"$flairVersion/flair/img/$flair.webp")

  def hcaptchaScript(re: lila.core.security.HcaptchaForm[?]): EsmList =
    re.enabled.so(jsModuleInit("bits.hcaptcha"))

  def analyseNvuiTag(using ctx: Context) = ctx.blind.option(EsmInit("analyse.nvui"))
