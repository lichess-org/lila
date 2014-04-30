package lila.app
package templating

import controllers.routes
import play.api.templates.Html

trait AssetHelper {

  val assetVersion = lila.api.Env.current.Net.AssetVersion

  def isProd: Boolean

  private val domain = lila.api.Env.current.Net.AssetDomain

  def staticUrl(path: String) = s"http://$domain${routes.Assets.at(path)}"

  def cssTag(name: String, staticDomain: Boolean = true) = cssAt("stylesheets/" + name, staticDomain)

  def cssVendorTag(name: String, staticDomain: Boolean = true) = cssAt("vendor/" + name, staticDomain)

  def cssAt(path: String, staticDomain: Boolean = true) = Html {
    val href = if (staticDomain) staticUrl(path) else routes.Assets.at(path)
    s"""<link href="$href?v=$assetVersion" type="text/css" rel="stylesheet"/>"""
  }

  def jsTag(name: String) = jsAt("javascripts/" + name)

  def jsTagCompiled(name: String) = if (isProd) jsAt("compiled/" + name) else jsTag(name)

  val jQueryTag = if (isProd) cdnOrLocal(
    cdn = "http://ajax.googleapis.com/ajax/libs/jquery/2.1.0/jquery.min.js",
    test = "window.jQuery",
    local = staticUrl("javascripts/vendor/jquery.min.js"))
  else jsTag("vendor/jquery.min.js")

  val highchartsTag = cdnOrLocal(
    cdn = "http://code.highcharts.com/3.0/highcharts.js",
    test = "window.Highcharts",
    local = staticUrl("vendor/highcharts/highcharts.js"))

  val highchartsMoreTag = cdnOrLocal(
    cdn = "http://code.highcharts.com/3.0/highcharts-more.js",
    test = "window.Highcharts",
    local = staticUrl("vendor/highcharts/highcharts-more.js"))

  private def cdnOrLocal(cdn: String, test: String, local: String) = Html {
    s"""<script src="$cdn"></script><script>$test || document.write('<script src="$local?v=$assetVersion">\\x3C/script>')</script>"""
  }

  def jsAt(path: String, static: Boolean = true) = Html {
    s"""<script src="${static.fold(staticUrl(path), path)}?v=$assetVersion"></script>"""
  }

  def embedJs(js: String): Html = Html(s"""<script type="text/javascript">/* <![CDATA[ */ $js /* ]]> */</script>""")
}
