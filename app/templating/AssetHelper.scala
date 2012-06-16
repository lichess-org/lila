package lila
package templating

import controllers._

import play.api.templates.Html

trait AssetHelper {

  val assetVersion = 33

  def cssTag(name: String) = css("stylesheets/" + name)

  def cssVendorTag(name: String) = css("vendor/" + name)

  private def css(path: String) = Html {
    """<link href="%s?v=%d" type="text/css" rel="stylesheet"/>"""
    .format(routes.Assets.at(path), assetVersion)
  }

  def jsTag(name: String) = js("javascripts/" + name)

  def jsVendorTag(name: String) = js("vendor/" + name)

  def js(path: String) = Html {
    """<script src="%s?v=%d"></script>"""
    .format(routes.Assets.at(path), assetVersion)
  }
}
