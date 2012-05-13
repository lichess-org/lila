package lila
package templating

import controllers._

import play.api.templates.Html

trait AssetHelper {

  def cssTag(name: String) = Html {
    """<link href="%s" type="text/css" rel="stylesheet"/>"""
    .format(routes.Assets.at("stylesheets/" + name))
  }

  def jsTag(name: String) = Html {
    """<script src="%s"></script>"""
    .format(routes.Assets.at("javascripts/" + name))
  }
}
