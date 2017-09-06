package lila.app
package templating

import play.twirl.api.Html

import lila.api.Context
import lila.common.AssetVersion

trait AssetHelper { self: I18nHelper =>

  def isProd: Boolean

  val assetDomain = lila.api.Env.current.Net.AssetDomain

  val assetBaseUrl = s"//$assetDomain"

  def cdnUrl(path: String) = s"$assetBaseUrl$path"
  def staticUrl(path: String) = s"$assetBaseUrl/assets/$path"

  def dbImageUrl(path: String) = s"$assetBaseUrl/image/$path"

  def cssTag(name: String, staticDomain: Boolean = true)(implicit ctx: Context): Html =
    cssAt("stylesheets/" + name, staticDomain)

  def cssVendorTag(name: String, staticDomain: Boolean = true)(implicit ctx: Context) =
    cssAt("vendor/" + name, staticDomain)

  def cssAt(path: String, staticDomain: Boolean, version: AssetVersion): Html = Html {
    val href = if (staticDomain) staticUrl(path) else s"/assets/$path"
    s"""<link href="$href?v=$version" type="text/css" rel="stylesheet"/>"""
  }
  def cssAt(path: String, staticDomain: Boolean = true)(implicit ctx: Context): Html =
    cssAt(path, staticDomain, ctx.pageData.assetVersion)

  def jsTag(name: String, async: Boolean = false)(implicit ctx: Context) =
    jsAt("javascripts/" + name, async = async)

  def jsTagCompiled(name: String)(implicit ctx: Context) =
    if (isProd) jsAt("compiled/" + name) else jsTag(name)

  def jsAt(path: String, static: Boolean, async: Boolean, version: AssetVersion): Html = Html {
    s"""<script${if (async) " async defer" else ""} src="${static.fold(staticUrl(path), path)}?v=$version"></script>"""
  }
  def jsAt(path: String, static: Boolean = true, async: Boolean = false)(implicit ctx: Context): Html =
    jsAt(path, static, async, ctx.pageData.assetVersion)

  val jQueryTag = Html {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.min.js")}"></script>"""
  }

  def roundTag(implicit ctx: Context) =
    jsAt(s"compiled/lichess.round${isProd ?? (".min")}.js", async = true)

  val highchartsTag = cdnOrLocal(
    cdn = "//code.highcharts.com/4.1.4/highcharts.js",
    test = "window.Highcharts",
    local = staticUrl("vendor/highcharts4/highcharts.js")
  )

  val highchartsLatestTag = cdnOrLocal(
    cdn = "//code.highcharts.com/4.2/highcharts.js",
    test = "window.Highcharts",
    local = staticUrl("vendor/highcharts-4.2.5/highcharts.js")
  )

  val highchartsMoreTag = Html {
    """<script src="//code.highcharts.com/4.1.4/highcharts-more.js"></script>"""
  }

  val tagmanagerTag = cdnOrLocal(
    cdn = "//cdnjs.cloudflare.com/ajax/libs/tagmanager/3.0.0/tagmanager.js",
    test = "$.tagsManager",
    local = staticUrl("vendor/tagmanager/tagmanager.js")
  )

  val typeaheadTag = cdnOrLocal(
    cdn = "//cdnjs.cloudflare.com/ajax/libs/typeahead.js/0.11.1/typeahead.bundle.min.js",
    test = "$.typeahead",
    local = staticUrl("javascripts/vendor/typeahead.bundle.min.js")
  )

  val fingerprintTag = Html {
    s"""<script async defer src="${staticUrl("javascripts/vendor/fp2.min.js")}"></script>"""
  }

  private def cdnOrLocal(cdn: String, test: String, local: String) = Html {
    if (isProd)
      s"""<script src="$cdn"></script><script>$test || document.write('<script src="$local">\\x3C/script>')</script>"""
    else
      s"""<script src="$local"></script>"""
  }

  def embedJs(js: String): Html = Html {
    val escaped = js.replace("</script", "<|script")
    s"""<script>$escaped</script>"""
  }
  def embedJs(js: Html): Html = embedJs(js.body)
}
