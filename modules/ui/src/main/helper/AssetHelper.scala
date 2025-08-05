package lila.ui

import play.api.libs.json.*

import lila.core.config.AssetBaseUrl
import lila.core.data.SafeJsonStr
import lila.ui.ScalatagsTemplate.*

trait AssetHelper:

  export lila.ui.Esm

  def assetBaseUrl: AssetBaseUrl
  def assetUrl(path: String): String
  def safeJsonValue(jsValue: JsValue): SafeJsonStr

  private val load = "site.asset.loadEsm"

  given Conversion[Esm, EsmList] with
    def apply(Esm: Esm): EsmList = List(Some(Esm))
  given Conversion[Option[Esm], EsmList] with
    def apply(esmOption: Option[Esm]): EsmList = List(esmOption)

  def esmInit(key: String): Esm =
    Esm(key, embedJsUnsafeLoadThen(s"$load('$key')"))
  def esmInit(key: String, json: SafeJsonStr): Esm =
    Esm(key, embedJsUnsafeLoadThen(s"$load('$key',{init:$json})"))
  def esmInit[A: Writes](key: String, value: A): Esm =
    esmInit(key, safeJsonValue(Json.toJson(value)))
  def esmInitObj(key: String, args: JsObject): Esm =
    esmInit(key, safeJsonValue(args))
  def esmInitObj(key: String, args: (String, Json.JsValueWrapper)*): Esm =
    esmInitObj(key, Json.obj(args*))
  def esmInitBit(fn: String, args: (String, Json.JsValueWrapper)*): Esm =
    esmInit("bits", safeJsonValue(Json.obj(args*) + ("fn" -> JsString(fn))))
  def esmPage(key: String): Esm =
    Esm(key, embedJsUnsafeLoadThen(s"site.asset.loadEsmPage('$key')"))

  val infiniteScrollEsmInit: Esm = esmInit("bits.infiniteScroll")
  val captchaEsm: Esm = Esm("bits.captcha")

  // load iife scripts in <head> and defer
  def iifeModule(path: String): Frag = script(deferAttr, src := assetUrl(path))

  def embedJsUnsafe(js: String): WithNonce[Frag] = nonce =>
    raw:
      val nonceAttr = nonce.so(n => s""" nonce="$n"""")
      s"""<script$nonceAttr>$js</script>"""

  def embedJsUnsafeLoadThen(js: String): WithNonce[Frag] = embedJsUnsafe(s"""site.load.then(()=>{$js})""")

  // bump flairs version if a flair is changed only (not added or removed)
  val flairVersion = "______3"

  def staticAssetUrl(path: String): String = s"$assetBaseUrl/assets/$path"

  def cdnUrl(path: String) = s"$assetBaseUrl$path"

  def flairSrc(flair: Flair): String = staticAssetUrl(s"$flairVersion/flair/img/$flair.webp")

  def iconFlair(flair: Flair): Tag = decorativeImg(cls := "icon-flair", src := flairSrc(flair))

  def fingerprintTag: EsmList = Esm("bits.fipr")

  def hcaptchaScript(re: lila.core.security.HcaptchaForm[?]): EsmList =
    re.enabled.so(esmInitBit("hcaptcha"))

  def analyseNvuiTag(using ctx: Context) = ctx.blind.option(Esm("analyse.nvui"))
