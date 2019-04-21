package lidraughts.app
package templating

import controllers.routes
import play.api.mvc.RequestHeader
import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.{ Nonce, AssetVersion, ContentSecurityPolicy }

trait AssetHelper { self: I18nHelper with SecurityHelper =>

  def isProd: Boolean

  val siteDomain = lidraughts.api.Env.current.Net.Domain
  val assetDomain = lidraughts.api.Env.current.Net.AssetDomain
  val socketDomain = lidraughts.api.Env.current.Net.SocketDomain

  val sameAssetDomain = siteDomain == assetDomain

  val assetBaseUrl = s"//$assetDomain"

  def assetVersion = AssetVersion.current

  def assetUrl(path: String): String = s"$assetBaseUrl/assets/_$assetVersion/$path"

  def cdnUrl(path: String) = s"$assetBaseUrl$path"
  def staticUrl(path: String) = s"$assetBaseUrl/assets/$path"

  def dbImageUrl(path: String) = s"$assetBaseUrl/image/$path"

  def cssTag(name: String)(implicit ctx: Context): Frag =
    cssTagWithTheme(name, ctx.currentBg)

  def cssTagWithTheme(name: String, theme: String): Frag =
    cssAt(s"css/$name.$theme.${if (isProd) "min" else "dev"}.css")

  def cssTagNoTheme(name: String)(implicit ctx: Context): Frag =
    cssAt(s"css/$name.${if (isProd) "min" else "dev"}.css")

  private def cssAt(path: String): Frag = raw {
    s"""<link href="${assetUrl(path)}" type="text/css" rel="stylesheet"/>"""
  }

  def jsTag(name: String, defer: Boolean = false): Frag =
    jsAt("javascripts/" + name, defer = defer)

  /* about async & defer, see https://flaviocopes.com/javascript-async-defer/
   * we want defer only, to ensure scripts are executed in order of declaration,
   * so that round.js doesn't run before site.js */
  def jsAt(path: String, defer: Boolean = false): Frag = script(
    defer option deferAttr,
    src := assetUrl(path)
  )

  val jQueryTag = raw {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.min.js")}"></script>"""
  }

  def roundTag = jsAt(s"compiled/lidraughts.round${isProd ?? (".min")}.js", defer = true)
  def roundNvuiTag(implicit ctx: Context) = ctx.blind option
    jsAt(s"compiled/lidraughts.round.nvui.min.js", defer = true)

  def analyseTag = jsAt(s"compiled/lidraughts.analyse${isProd ?? (".min")}.js")
  def analyseNvuiTag(implicit ctx: Context) = ctx.blind option
    jsAt(s"compiled/lidraughts.analyse.nvui.min.js")

  def captchaTag = jsAt(s"compiled/captcha.js")

  val highchartsLatestTag = raw {
    s"""<script src="${staticUrl("vendor/highcharts-4.2.5/highcharts.js")}"></script>"""
  }

  val highchartsMoreTag = raw {
    s"""<script src="${staticUrl("vendor/highcharts-4.2.5/highcharts-more.js")}"></script>"""
  }

  val typeaheadTag = raw {
    s"""<script src="${staticUrl("javascripts/vendor/typeahead.bundle.min.js")}"></script>"""
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
      embedJsUnsafe("""window.prismic={endpoint:'https://lidraughts.prismic.io/api/v2'}""").render ++
        """<script type="text/javascript" src="//static.cdn.prismic.io/prismic.min.js"></script>"""
    }
  }

  def basicCsp(implicit req: RequestHeader): ContentSecurityPolicy = {
    val assets = if (req.secure) "https://" + assetDomain else assetDomain
    val socket = (if (req.secure) "wss://" else "ws://") + socketDomain + (if (socketDomain.contains(":")) "" else ":*")
    ContentSecurityPolicy(
      defaultSrc = List("'self'", assets),
      connectSrc = List("'self'", assets, socket, lidraughts.api.Env.current.ExplorerEndpoint, lidraughts.api.Env.current.TablebaseEndpoint),
      styleSrc = List("'self'", "'unsafe-inline'", assets),
      fontSrc = List("'self'", assetDomain, "https://fonts.gstatic.com"),
      frameSrc = List("'self'", assets, "https://www.youtube.com", "https://player.twitch.tv"),
      workerSrc = List("'self'", assets),
      imgSrc = List("data:", "*"),
      scriptSrc = List("'self'", assets),
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
