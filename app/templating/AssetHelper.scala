package lila.app
package templating

import play.api.mvc.RequestHeader

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.common.{ AssetVersion, ContentSecurityPolicy, Nonce }

trait AssetHelper { self: I18nHelper with SecurityHelper =>

  def isProd: Boolean
  def isStage: Boolean

  def minifiedAssets = isProd || isStage

  def netDomain: lila.common.config.NetDomain
  lazy val assetDomain    = env.net.assetDomain
  lazy val socketDomains  = env.net.socketDomains
  lazy val vapidPublicKey = env.push.vapidPublicKey

  lazy val sameAssetDomain = netDomain.value == assetDomain.value

  lazy val assetBaseUrl = env.net.assetBaseUrl

  def assetVersion = AssetVersion.current

  def assetUrl(path: String): String = s"$assetBaseUrl/assets/_$assetVersion/$path"

  def cdnUrl(path: String)    = s"$assetBaseUrl$path"
  def staticUrl(path: String) = s"$assetBaseUrl/assets/$path"

  def dbImageUrl(path: String) = s"$assetBaseUrl/image/$path"

  def cssTag(name: String)(implicit ctx: Context): Frag =
    cssTagWithTheme(name, ctx.currentBg)

  def cssTagWithTheme(name: String, theme: String): Frag =
    cssAt(s"css/$name.$theme.${if (minifiedAssets) "min" else "dev"}.css")

  def cssTagNoTheme(name: String): Frag =
    cssAt(s"css/$name.${if (minifiedAssets) "min" else "dev"}.css")

  def cssAt(path: String): Frag =
    link(href := assetUrl(path), tpe := "text/css", rel := "stylesheet")

  def jsTag(name: String, defer: Boolean = false): Frag =
    jsAt("javascripts/" + name, defer = defer)

  /* about async & defer, see https://flaviocopes.com/javascript-async-defer/
   * we want defer only, to ensure scripts are executed in order of declaration,
   * so that round.js doesn't run before site.js */
  def jsAt(path: String, defer: Boolean = false): Frag =
    script(
      defer option deferAttr,
      src := assetUrl(path)
    )

  def jsModule(name: String, defer: Boolean = false): Frag =
    jsAt(s"compiled/lishogi.$name${minifiedAssets ?? ".min"}.js", defer = defer)

  lazy val jQueryTag = raw {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.min.js")}"></script>"""
  }

  def roundTag = jsAt(s"compiled/lishogi.round${minifiedAssets ?? ".min"}.js", defer = true)
  def roundNvuiTag(implicit ctx: Context) =
    ctx.blind option
      jsAt(s"compiled/lishogi.round.nvui${minifiedAssets ?? ".min"}.js", defer = true)

  def analyseTag = jsAt(s"compiled/lishogi.analyse${minifiedAssets ?? ".min"}.js")
  def analyseNvuiTag(implicit ctx: Context) =
    ctx.blind option
      jsAt(s"compiled/lishogi.analyse.nvui${minifiedAssets ?? ".min"}.js")

  def captchaTag = jsAt(s"compiled/captcha.js")

  lazy val highchartsLatestTag = raw {
    s"""<script src="${staticUrl("vendor/highcharts-4.2.5/highcharts.js")}"></script>"""
  }

  lazy val highchartsMoreTag = raw {
    s"""<script src="${staticUrl("vendor/highcharts-4.2.5/highcharts-more.js")}"></script>"""
  }

  lazy val fingerprintTag = raw {
    s"""<script defer src="${staticUrl("javascripts/fipr.js")}"></script>"""
  }

  lazy val flatpickrTag = raw {
    s"""<script defer src="${staticUrl("javascripts/vendor/flatpickr.min.js")}"></script>"""
  }

  lazy val tagifyTag = raw {
    s"""<script src="${staticUrl("vendor/tagify/tagify.min.js")}"></script>"""
  }

  def delayFlatpickrStartUTC(implicit ctx: Context) =
    embedJsUnsafe {
      """$(function() { setTimeout(function() { $(".flatpickr").flatpickr(); }, 1000) });"""
    }

  def delayFlatpickrStartLocal(implicit ctx: Context) =
    embedJsUnsafe {
      """$(function() { setTimeout(function() { $(".flatpickr").flatpickr({
  maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31),
  dateFormat: 'Z',
  altInput: true,
  altFormat: 'Y-m-d h:i K',
  disableMobile: true
}); }, 1000) });"""
    }

  lazy val infiniteScrollTag = jsTag("vendor/jquery.infinitescroll.min.js")

  def prismicJs(implicit ctx: Context): Frag =
    raw {
      isGranted(_.Prismic) ?? {
        embedJsUnsafe("""window.prismic={endpoint:'https://lishogi.prismic.io/api/v2'}""").render ++
          """<script type="text/javascript" src="//static.cdn.prismic.io/prismic.min.js"></script>"""
      }
    }

  def basicCsp(implicit req: RequestHeader): ContentSecurityPolicy = {
    val assets = if (req.secure) s"https://$assetDomain" else assetDomain.value
    val sockets = socketDomains map { socketDomain =>
      val protocol = if (req.secure) "wss://" else "ws://"
      s"$protocol$socketDomain"
    }
    ContentSecurityPolicy(
      defaultSrc = List("'self'", assets),
      connectSrc = "'self'" :: assets :: sockets ::: env.insightsEndpoint :: Nil,
      styleSrc = List("'self'", "'unsafe-inline'", assets),
      fontSrc = List("'self'", assetDomain.value, "https://fonts.gstatic.com"),
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

  def embedJsUnsafe(js: String)(implicit ctx: Context): Frag =
    raw {
      val nonce = ctx.nonce ?? { nonce =>
        s""" nonce="$nonce""""
      }
      s"""<script$nonce>$js</script>"""
    }

  def embedJsUnsafe(js: String, nonce: Nonce): Frag =
    raw {
      s"""<script nonce="$nonce">$js</script>"""
    }
}
