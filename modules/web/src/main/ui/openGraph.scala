package lila.web
package ui

import lila.ui.*

import ScalatagsTemplate.*

def openGraph(graph: OpenGraph): Frag =
  og.frags(graph) ::: twitter.frags(graph)

private object og:

  private val property = attr("property")

  private def tag(name: String, value: String) =
    meta(
      property := s"og:$name",
      content  := value
    )

  def frags(graph: OpenGraph): List[Frag] =
    import graph.*
    List(
      "title"       -> title,
      "description" -> description,
      "url"         -> url,
      "type"        -> `type`,
      "site_name"   -> siteName
    ).map(tag) ::: image.map { tag("image", _) }.toList

private object twitter:

  private def tag(n: String, value: String) =
    meta(
      name    := s"twitter:$n",
      content := value
    )

  def frags(graph: OpenGraph): List[Frag] =
    import graph.*
    List(
      "card"        -> "summary",
      "title"       -> title,
      "description" -> description
    ).map(tag) ::: (twitterImage.orElse(image)).map { tag("image", _) }.toList
