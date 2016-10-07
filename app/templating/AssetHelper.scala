package lila.app
package templating

import controllers.routes
import play.api.i18n.Lang
import play.twirl.api.Html

trait AssetHelper { self: I18nHelper =>

  def assetVersion = lila.api.Env.current.assetVersion.get

  def isProd: Boolean

  val assetDomain = lila.api.Env.current.Net.AssetDomain

  val assetBaseUrl = s"//$assetDomain"

  def cdnUrl(path: String) = s"$assetBaseUrl$path"
  def staticUrl(path: String) = s"$assetBaseUrl${routes.Assets.at(path)}"

  def dbImageUrl(path: String) = s"$assetBaseUrl/image/$path"

  def cssTag(name: String, staticDomain: Boolean = true) = cssAt("stylesheets/" + name, staticDomain)

  def cssVendorTag(name: String, staticDomain: Boolean = true) = cssAt("vendor/" + name, staticDomain)

  def cssAt(path: String, staticDomain: Boolean = true) = Html {
    val href = if (staticDomain) staticUrl(path) else routes.Assets.at(path)
    s"""<link href="$href?v=$assetVersion" type="text/css" rel="stylesheet"/>"""
  }

  def jsTag(name: String) = jsAt("javascripts/" + name)

  def jsTagCompiled(name: String) = if (isProd) jsAt("compiled/" + name) else jsTag(name)

  val jQueryTag = cdnOrLocal(
    cdn = "//cdnjs.cloudflare.com/ajax/libs/jquery/3.1.1/jquery.min.js",
    test = "window.jQuery",
    local = staticUrl("javascripts/vendor/jquery.min.js"))

  val highchartsTag = cdnOrLocal(
    cdn = "//code.highcharts.com/4.1.4/highcharts.js",
    test = "window.Highcharts",
    local = staticUrl("vendor/highcharts4/highcharts.js"))

  val highchartsLatestTag = cdnOrLocal(
    cdn = "//code.highcharts.com/4.2/highcharts.js",
    test = "window.Highcharts",
    local = staticUrl("vendor/highcharts4/highcharts-4.2.5.js"))

  val highchartsMoreTag = Html {
    """<script src="//code.highcharts.com/4.1.4/highcharts-more.js"></script>"""
  }

  val momentjsTag = cdnOrLocal(
    cdn = "//cdnjs.cloudflare.com/ajax/libs/moment.js/2.10.6/moment.min.js",
    test = "window.moment",
    local = staticUrl("vendor/moment/min/moment.min.js"))

  def momentLangTag(implicit ctx: lila.api.Context) = {
    val l = lang(ctx)
    ((l.language, l.country.toLowerCase) match {
      case ("en", "us")               => none
      case ("en", "au" | "ca" | "gb") => l.code.some
      case ("pt", "br")               => l.code.some
      case ("zh", "tw")               => l.code.some
      case ("zh", _)                  => "zh-cn".some
      case ("ar", "ma" | "sa" | "tn") => l.code.some
      case ("fr", "ca")               => l.code.some
      case _                          => l.language.some
    }).fold(Html("")) { locale =>
      jsAt(s"vendor/moment/locale/${locale.toLowerCase}.js", static = true)
    }
  }

  val tagmanagerTag = cdnOrLocal(
    cdn = "//cdnjs.cloudflare.com/ajax/libs/tagmanager/3.0.0/tagmanager.js",
    test = "$.tagsManager",
    local = staticUrl("vendor/tagmanager/tagmanager.js"))

  val typeaheadTag = cdnOrLocal(
    cdn = "//cdnjs.cloudflare.com/ajax/libs/typeahead.js/0.11.1/typeahead.bundle.min.js",
    test = "$.typeahead",
    local = staticUrl("javascripts/vendor/typeahead.bundle.min.js"))

  val fingerprintTag = Html {
    """<script src="//cdn.jsdelivr.net/fingerprintjs2/0.7/fingerprint2.min.js"></script>"""
  }

  private def cdnOrLocal(cdn: String, test: String, local: String) = Html {
    if (isProd)
      s"""<script src="$cdn"></script><script>$test || document.write('<script src="$local">\\x3C/script>')</script>"""
    else
      s"""<script src="$local"></script>"""
  }

  def jsAt(path: String, static: Boolean = true) = Html {
    s"""<script src="${static.fold(staticUrl(path), path)}?v=$assetVersion"></script>"""
  }

  def embedJs(js: String): Html = Html {
    val escaped = js.replace("</script", "<|script")
    s"""<script>$escaped</script>"""
  }
  def embedJs(js: Html): Html = embedJs(js.body)
}
