package lila.ui

import play.api.libs.json.*

import lila.core.config.{ BaseUrl, ImageGetOrigin, RouteUrl }
import lila.core.data.SafeJsonStr
import lila.ui.ScalatagsTemplate.{ *, given }

trait AssetHelper:

  export lila.ui.Esm

  def netBaseUrl: BaseUrl
  def routeUrl: RouteUrl
  def assetFullUrl(path: String): Url
  def assetPath(path: String): Path
  def safeJsonValue(jsValue: JsValue): SafeJsonStr
  def imageGetOrigin: ImageGetOrigin

  given ImageGetOrigin = imageGetOrigin

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
  def iifeModule(path: String): Frag = script(deferAttr, src := assetPath(path))

  def embedJsUnsafe(js: String): WithNonce[Frag] = nonce =>
    raw:
      val nonceAttr = nonce.so(n => s""" nonce="$n"""")
      s"""<script$nonceAttr>$js</script>"""

  def embedJsUnsafeLoadThen(js: String): WithNonce[Frag] = embedJsUnsafe(s"""site.load.then(()=>{$js})""")

  // bump flairs version if a flair is changed only (not added or removed)
  val flairVersion = "______5"

  // bump fide fed version if a fide fed is changed only (not added or removed)
  val fideFedVersion = "______3"

  def staticAssetPath(path: String): Path = Path(s"/assets/$path")

  def staticCompiledPath(path: String): Path = staticAssetPath(s"compiled/$path")

  def flairSrc(flair: Flair): Path = staticAssetPath(s"$flairVersion/flair/img/$flair.webp")

  def iconFlair(flair: Flair): Tag = decorativeImg(cls := "icon-flair", src := flairSrc(flair))

  def imagePreload(path: Path) = imagePreloadAny(path.value)

  def imagePreloadUrl(url: Url) = imagePreloadAny(url.value)

  private def imagePreloadAny(pathOrUrl: String) =
    raw(s"""<link rel="preload" href="$pathOrUrl" as="image" fetchpriority="high">""")

  def preload(url: Path, as: String, crossorigin: Boolean, tpe: Option[String] = None) =
    val linkType = tpe.so(t => s""" type="$t"""")
    raw:
      s"""<link rel="preload" href="$url" as="$as"$linkType${crossorigin.so(" crossorigin")}>"""

  def fingerprintTag: EsmList = Esm("bits.fipr")

  def analyseNvuiTag(using ctx: Context) = ctx.blind.option(Esm("analyse.nvui"))

  def pathUrl(path: String): Url = Url(s"${netBaseUrl}$path")

  def fenThumbnailUrl(
      fen: chess.format.StandardFen,
      color: Option[chess.Color] = None,
      variant: chess.variant.Variant = chess.variant.Standard
  )(using ctx: Context): Url = Url:
    routes.Export.fenThumbnail(
      fen.value,
      color,
      none,
      Option.when(variant.exotic)(variant.key),
      ctx.pref.theme.some,
      ctx.pref.pieceSet.some
    )
