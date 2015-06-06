package lila.app
package templating

import controllers.routes
import play.twirl.api.Html

trait AssetHelper { self: I18nHelper =>

  val assetVersion = lila.api.Env.current.Net.AssetVersion

  def isProd: Boolean

  val assetDomain = lila.api.Env.current.Net.AssetDomain

  val assetBaseUrl = s"http://$assetDomain"

  def cdnUrl(path: String) = s"$assetBaseUrl$path"
  def staticUrl(path: String) = s"$assetBaseUrl${routes.Assets.at(path)}"

  def cssTag(name: String, staticDomain: Boolean = true) = cssAt("stylesheets/" + name, staticDomain)

  def cssVendorTag(name: String, staticDomain: Boolean = true) = cssAt("vendor/" + name, staticDomain)

  def cssAt(path: String, staticDomain: Boolean = true) = Html {
    val href = if (staticDomain) staticUrl(path) else routes.Assets.at(path)
    s"""<link href="$href?v=$assetVersion" type="text/css" rel="stylesheet"/>"""
  }

  def jsTag(name: String) = jsAt("javascripts/" + name)

  def jsTagCompiled(name: String) = if (isProd) jsAt("compiled/" + name) else jsTag(name)

  val jQueryTag = cdnOrLocal(
    cdn = "//cdnjs.cloudflare.com/ajax/libs/jquery/2.1.4/jquery.min.js",
    test = "window.jQuery",
    local = staticUrl("javascripts/vendor/jquery.min.js"))

  val highchartsTag = cdnOrLocal(
    cdn = "http://code.highcharts.com/4.1.4/highcharts.js",
    test = "window.Highcharts",
    local = staticUrl("vendor/highcharts4/highcharts.js"))

  val highchartsMoreTag = cdnOrLocal(
    cdn = "http://code.highcharts.com/4.1.4/highcharts-more.js",
    test = "window.Highcharts",
    local = staticUrl("vendor/highcharts4/highcharts-more.js"))

  val highstockTag = cdnOrLocal(
    cdn = "http://code.highcharts.com/stock/2.1/highstock.js",
    test = "window.Highcharts.StockChart",
    local = staticUrl("vendor/highcharts4/highstock.js"))

  val momentjsTag = cdnOrLocal(
    cdn = "http://cdnjs.cloudflare.com/ajax/libs/moment.js/2.8.4/moment.min.js",
    test = "window.moment",
    local = staticUrl("vendor/moment/min/moment.min.js"))

  def momentLangTag(implicit ctx: lila.api.Context) = (lang(ctx).language match {
    case "en" => none
    case "pt" => "pt-br".some
    case "zh" => "zh-cn".some
    case l    => l.some
  }).fold(Html("")) { l =>
    jsAt(s"vendor/moment/locale/$l.js", static = true)
  }

  val tagmanagerTag = cdnOrLocal(
    cdn = "http://cdnjs.cloudflare.com/ajax/libs/tagmanager/3.0.0/tagmanager.js",
    test = "$.tagsManager",
    local = staticUrl("vendor/tagmanager/tagmanager.js"))

  val typeaheadTag = cdnOrLocal(
    cdn = "http://cdnjs.cloudflare.com/ajax/libs/typeahead.js/0.10.2/typeahead.bundle.min.js",
    test = "$.typeahead",
    local = staticUrl("vendor/typeahead.bundle.min.js"))

  private def cdnOrLocal(cdn: String, test: String, local: String) = Html {
    if (isProd)
      s"""<script src="$cdn"></script><script>$test || document.write('<script src="$local">\\x3C/script>')</script>"""
    else
      s"""<script src="$local"></script>"""
  }

  def jsAt(path: String, static: Boolean = true) = Html {
    s"""<script src="${static.fold(staticUrl(path), path)}?v=$assetVersion"></script>"""
  }

  def embedJs(js: String): Html = Html(s"""<script>/* <![CDATA[ */ $js /* ]]> */</script>""")
  def embedJs(js: Html): Html = embedJs(js.body)
}
