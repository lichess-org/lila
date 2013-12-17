package lila.app
package templating

import controllers.routes
import play.api.templates.Html

trait AssetHelper {

  val assetVersion = lila.api.Env.current.Net.AssetVersion

  def isProd: Boolean

  private val domain = lila.api.Env.current.Net.AssetDomain

  private def url(path: String) = "http://" + domain + path

  def cssTag(name: String) = css("stylesheets/" + name)

  def cssVendorTag(name: String) = css("vendor/" + name)

  private def css(path: String) = Html {
    s"""<link href="${url(routes.Assets.at(path).toString)}?v=$assetVersion" type="text/css" rel="stylesheet"/>"""
  }

  def jsTag(name: String) = js("javascripts/" + name)

  def jsTagCompiled(name: String) = isProd ? js("compiled/" + name) | jsTag(name)

  def jsVendorTag(name: String) = js("vendor/" + name)

  lazy val highchartsTag = cdnOrLocal(
    cdn = "http://code.highcharts.com/3.0/highcharts.js",
    test = "window.Highcharts",
    local = routes.Assets.at("vendor/highcharts/highcharts.js").toString)

  lazy val highchartsMoreTag = cdnOrLocal(
    cdn = "http://code.highcharts.com/3.0/highcharts-more.js",
    test = "window.Highcharts",
    local = routes.Assets.at("vendor/highcharts/highcharts-more.js").toString)

  lazy val highstockTag = cdnOrLocal(
    cdn = "http://code.highcharts.com/stock/3.0/highstock.js",
    test = "window.Highcharts",
    local = routes.Assets.at("vendor/highstock/highstock.js").toString)

  private def cdnOrLocal(cdn: String, test: String, local: String) = Html {
    s"""<script src="$cdn"></script><script>$test || document.write('<script src="$local?v=$assetVersion">\\x3C/script>')</script>"""
  }

  private def js(path: String) = jsAt(routes.Assets.at(path).toString)

  def jsAt(path: String, static: Boolean = true) = Html {
    s"""<script src="${static.fold(url(path), path)}?v=$assetVersion"></script>"""
  }

  def embedJs(js: String): Html = Html(s"""<script type="text/javascript">/* <![CDATA[ */ $js /* ]]> */</script>""")
}
