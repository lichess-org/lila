package lila.app
package templating

import play.api.templates.Html

import controllers.routes

trait AssetHelper {

  val assetVersion = 66

  private val domain = lila.api.Env.current.Net.AssetDomain

  private def url(path: String) = "http://%s/%s".format(domain, path)

  def cssTag(name: String) = css("stylesheets/" + name)

  def cssVendorTag(name: String) = css("vendor/" + name)

  private def css(path: String) = Html {
    """<link href="%s?v=%d" type="text/css" rel="stylesheet"/>"""
      .format(url(routes.Assets.at(path).toString), assetVersion)
  }

  def jsTag(name: String) = js("javascripts/" + name)

  def jsTagC(name: String) = js("compiled/" + name)

  def jsVendorTag(name: String) = js("vendor/" + name)

  private def js(path: String) = jsAt(routes.Assets.at(path).toString)

  def jsAt(path: String) = Html {
    """<script src="%s?v=%d"></script>""".format(url(path), assetVersion)
  }

  def embedJs(js: String): Html = Html("""<script type="text/javascript">
/* <![CDATA[ */
%s
/* ]]> */
</script>""" format js)
}
