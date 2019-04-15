package lila.app
package templating

import controllers.routes
import play.api.mvc.RequestHeader
import play.twirl.api.Html

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.common.{ Nonce, AssetVersion, ContentSecurityPolicy }

trait AssetHelper { self: I18nHelper with SecurityHelper =>

  def isProd: Boolean

  val assetDomain = lila.api.Env.current.Net.AssetDomain
  val socketDomain = lila.api.Env.current.Net.SocketDomain

  val assetBaseUrl = s"//$assetDomain"

  def assetVersion = AssetVersion.current

  def assetUrl(path: String): String = s"$assetBaseUrl/assets/_$assetVersion/$path"

  def cdnUrl(path: String) = s"$assetBaseUrl$path"
  def staticUrl(path: String) = s"$assetBaseUrl/assets/$path"

  def dbImageUrl(path: String) = s"$assetBaseUrl/image/$path"

  def responsiveCssTag(name: String)(implicit ctx: Context): Frag =
    responsiveCssTagWithTheme(name, ctx.currentBg)

  def responsiveCssTagWithTheme(name: String, theme: String): Frag =
    cssAt(s"css/$name.$theme.${if (isProd) "min" else "dev"}.css")

  def responsiveCssTagNoTheme(name: String)(implicit ctx: Context): Frag =
    cssAt(s"css/$name.${if (isProd) "min" else "dev"}.css")

  def cssTag(name: String): Frag = cssAt("stylesheets/" + name)

  def cssTags(names: String*): Frag = names map cssTag

  def cssTags(names: List[(String, Boolean)]): Frag =
    cssTags(names.collect { case (k, true) => k }: _*)

  def cssVendorTag(name: String): Frag = cssAt("vendor/" + name)

  def cssAt(path: String): Frag = raw {
    s"""<link href="${assetUrl(path)}" type="text/css" rel="stylesheet"/>"""
  }

  def jsTag(name: String, async: Boolean = false) =
    jsAt("javascripts/" + name, async = async)

  /* about async & defer, see https://flaviocopes.com/javascript-async-defer/
   * we want defer only, to ensure scripts are executed in order of declaration,
   * so that round.js doesn't run before site.js */
  def jsAt(path: String, async: Boolean = false): Html = Html {
    val src = assetUrl(path)
    s"""<script${async ?? " defer"} src="$src"></script>"""
  }

  val jQueryTag = raw {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.min.js")}"></script>"""
  }

  def roundTag = jsAt(s"compiled/lichess.round${isProd ?? (".min")}.js", async = true)
  def roundNvuiTag(implicit ctx: Context) = ctx.blind option
    jsAt(s"compiled/lichess.round.nvui.min.js", async = true)

  def analyseTag = jsAt(s"compiled/lichess.analyse${isProd ?? (".min")}.js")
  def analyseNvuiTag(implicit ctx: Context) = ctx.blind option
    jsAt(s"compiled/lichess.analyse.nvui.min.js")

  def captchaTag = jsAt(s"compiled/captcha.js")

  val highchartsLatestTag = raw {
    s"""<script src="${staticUrl("vendor/highcharts-4.2.5/highcharts.js")}"></script>"""
  }

  val highchartsMoreTag = raw {
    s"""<script src="${staticUrl("vendor/highcharts-4.2.5/highcharts-more.js")}"></script>"""
  }

  val fingerprintTag = raw {
    s"""<script async src="${staticUrl("javascripts/vendor/fp2.min.js")}"></script>"""
  }

  val flatpickrTag = raw {
    s"""<script defer src="${staticUrl("javascripts/vendor/flatpickr.min.js")}"></script>"""
  }

  val nonAsyncFlatpickrTag = raw {
    s"""<script defer src="${staticUrl("javascripts/vendor/flatpickr.min.js")}"></script>"""
  }

  def delayFlatpickrStart(implicit ctx: Context) = embedJs {
    """$(function() { setTimeout(function() { $(".flatpickr").flatpickr(); }, 2000) });"""
  }

  val infiniteScrollTag = jsTag("vendor/jquery.infinitescroll.min.js")

  def prismicJs(implicit ctx: Context): Frag = raw {
    isGranted(_.Prismic) ?? {
      embedJsUnsafe("""window.prismic={endpoint:'https://lichess.prismic.io/api/v2'}""").render ++
        """<script type="text/javascript" src="//static.cdn.prismic.io/prismic.min.js"></script>"""
    }
  }

  def basicCsp(implicit req: RequestHeader): ContentSecurityPolicy = {
    val assets = if (req.secure) "https://" + assetDomain else assetDomain
    val socket = (if (req.secure) "wss://" else "ws://") + socketDomain + (if (socketDomain.contains(":")) "" else ":*")
    ContentSecurityPolicy(
      defaultSrc = List("'self'", assets),
      connectSrc = List("'self'", assets, socket, lila.api.Env.current.ExplorerEndpoint, lila.api.Env.current.TablebaseEndpoint),
      styleSrc = List("'self'", "'unsafe-inline'", assets, "https://fonts.googleapis.com"),
      fontSrc = List("'self'", assetDomain, "https://fonts.gstatic.com"),
      frameSrc = List("'self'", assets, "https://www.youtube.com"),
      workerSrc = List("'self'", assets),
      imgSrc = List("data:", "*"),
      scriptSrc = List("'self'", "'unsafe-eval'", assets), // unsafe-eval for WebAssembly (wasmx)
      baseUri = List("'none'")
    )
  }

  def defaultCsp(implicit ctx: Context): ContentSecurityPolicy = {
    val csp = basicCsp(ctx.req)
    ctx.nonce.fold(csp)(csp.withNonce(_))
  }

  def embedJsUnsafe(js: String)(implicit ctx: Context): Frag = raw {
    val nonce = ctx.nonce ?? { nonce => s""" nonce="$nonce"""" }
    s"""<script$nonce>$js</script>"""
  }

  def embedJs(js: Frag)(implicit ctx: Context): Frag = embedJsUnsafe(js.render)
  def embedJs(js: String)(implicit ctx: Context): Frag = embedJsUnsafe(js)

  def embedJs(js: String, nonce: Nonce): Frag = raw {
    s"""<script nonce="$nonce">$js</script>"""
  }
}
