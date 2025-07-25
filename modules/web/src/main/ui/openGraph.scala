package lila.web
package ui

import lila.ui.*

import ScalatagsTemplate.*

def openGraph(graph: OpenGraph): List[Frag] =
  import graph.*
  val property = attr("property")
  def tag(name: String, value: String) =
    meta(
      property := s"og:$name",
      content := value
    )
  List(
    "title" -> title,
    "description" -> description,
    "url" -> url,
    "type" -> `type`,
    "site_name" -> siteName
  ).map(tag) ::: image.map { tag("image", _) }.toList
