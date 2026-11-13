package lila.web
package ui

import play.api.i18n.Lang

import lila.ui.*
import lila.ui.ScalatagsTemplate.*

object OpenGraph:

  private val property = attr("property")

  private def tag(name: String, value: String) =
    meta(property := name, content := value)

  private def ogTag(name: String, value: String) =
    tag(s"og:$name", value)

  def render(graph: OpenGraph)(using lang: Lang): List[Frag] =
    import graph.*
    List(
      "title" -> title,
      "description" -> description,
      "url" -> url.value,
      "type" -> `type`,
      "site_name" -> siteName,
      "locale" -> lang.code
    ).map(ogTag) :::
      image.map(i => ogTag("image", i.value)).toList :::
      extra.map(tag)
