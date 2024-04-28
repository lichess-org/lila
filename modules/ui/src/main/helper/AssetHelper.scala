package lila.ui

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.AssetBaseUrl

trait AssetHelper:

  export lila.ui.EsmInit

  def assetBaseUrl: AssetBaseUrl

  def cssTag(key: String): Frag

  val infiniteScrollEsmInit: EsmInit
  val captchaEsmInit: EsmInit

  extension (l: Layout)
    def cssTag(keys: String*): Layout =
      keys.foldLeft(l)((l, key) => l.css(AssetHelper.this.cssTag(key)))

  def embedJsUnsafe(js: String): WithNonce[Frag] = nonce =>
    raw:
      val nonceAttr = nonce.so(n => s""" nonce="$n"""")
      s"""<script$nonceAttr>$js</script>"""

  private val onLoadFunction = "site.load.then"

  def embedJsUnsafeLoadThen(js: String): WithNonce[Frag] =
    embedJsUnsafe(s"""$onLoadFunction(()=>{$js})""")

  def embedJsUnsafeLoadThen(js: String, nonce: Nonce): Frag =
    embedJsUnsafeLoadThen(js)(nonce.some)

  // bump flairs version if a flair is changed only (not added or removed)
  val flairVersion = "______2"

  def staticAssetUrl(path: String): String = s"$assetBaseUrl/assets/$path"

  def cdnUrl(path: String) = s"$assetBaseUrl$path"

  def flairSrc(flair: Flair): String = staticAssetUrl(s"$flairVersion/flair/img/$flair.webp")
