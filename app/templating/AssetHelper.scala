package lila.app
package templating

import controllers.routes
import play.api.mvc.RequestHeader
import play.twirl.api.Html

import lila.api.Context
import lila.common.{ AssetVersion, ContentSecurityPolicy }

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

  def responsiveCssTag(name: String)(implicit ctx: Context): Html =
    cssAt(s"css/$name.${ctx.currentBg}.${if (isProd) "min" else "dev"}.css")

  def responsiveCssTagNoTheme(name: String)(implicit ctx: Context): Html =
    cssAt(s"css/$name.${if (isProd) "min" else "dev"}.css")

  def cssTag(name: String): Html = cssAt("stylesheets/" + name)

  def cssTags(names: String*): Html = Html {
    names.map { name =>
      cssTag(name).body
    } mkString ""
  }
  def cssTags(names: List[(String, Boolean)]): Html =
    cssTags(names.collect { case (k, true) => k }: _*)

  def cssVendorTag(name: String) = cssAt("vendor/" + name)

  def cssAt(path: String): Html = Html {
    s"""<link href="${assetUrl(path)}" type="text/css" rel="stylesheet"/>"""
  }

  def jsTag(name: String, async: Boolean = false) =
    jsAt("javascripts/" + name, async = async)

  def jsAt(path: String, async: Boolean = false): Html = Html {
    val src = assetUrl(path)
    s"""<script${if (async) " async defer" else ""} src="$src"></script>"""
  }

  val jQueryTag = Html {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.min.js")}"></script>"""
  }

  def roundTag = jsAt(s"compiled/lichess.round${isProd ?? (".min")}.js", async = true)
  def roundNvuiTag(implicit ctx: Context) = ctx.blind option
    jsAt(s"compiled/lichess.round.nvui.min.js", async = true)

  def analyseTag = jsAt(s"compiled/lichess.analyse${isProd ?? (".min")}.js")
  def analyseNvuiTag(implicit ctx: Context) = ctx.blind option
    jsAt(s"compiled/lichess.analyse.nvui.min.js")

  val highchartsLatestTag = Html {
    s"""<script src="${staticUrl("vendor/highcharts-4.2.5/highcharts.js")}"></script>"""
  }

  val highchartsMoreTag = Html {
    s"""<script src="${staticUrl("vendor/highcharts-4.2.5/highcharts-more.js")}"></script>"""
  }

  val tagmanagerTag = Html {
    s"""<script src="${staticUrl("vendor/tagmanager/tagmanager.js")}"></script>"""
  }

  val typeaheadTag = Html {
    s"""<script src="${staticUrl("javascripts/vendor/typeahead.bundle.min.js")}"></script>"""
  }

  val fingerprintTag = Html {
    s"""<script async defer src="${staticUrl("javascripts/vendor/fp2.min.js")}"></script>"""
  }

  val flatpickrTag = Html {
    s"""<script async defer src="${staticUrl("javascripts/vendor/flatpickr.min.js")}"></script>"""
  }
  def delayFlatpickrStart(implicit ctx: Context) = embedJs {
    """$(function() { setTimeout(function() { $(".flatpickr").flatpickr(); }, 2000) });"""
  }

  val infiniteScrollTag = jsTag("vendor/jquery.infinitescroll.min.js")

  def prismicJs(implicit ctx: Context) = Html {
    isGranted(_.Prismic) ?? {
      embedJsUnsafe("""window.prismic={endpoint:'https://lichess.prismic.io/api/v2'}""").body ++
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

  def embedJsUnsafe(js: String)(implicit ctx: Context): Html = Html {
    val nonce = ctx.nonce ?? { nonce => s""" nonce="$nonce"""" }
    s"""<script$nonce>$js</script>"""
  }
  def embedJsUnsafe(js: scalatags.Text.RawFrag)(implicit ctx: Context): scalatags.Text.RawFrag = scalatags.Text.all.raw {
    val nonce = ctx.nonce ?? { nonce => s""" nonce="$nonce"""" }
    s"""<script$nonce>$js</script>"""
  }

  def embedJs(js: Html)(implicit ctx: Context): Html = embedJsUnsafe(js.body)
  def embedJs(js: String)(implicit ctx: Context): Html = embedJsUnsafe(js)
}
