package lila.app
package ui

import lila.common.String.html.escapeHtml

case class OpenGraph(
    title: String,
    description: String,
    url: String,
    `type`: String = "website",
    image: Option[String] = None,
    siteName: String = "lichess.org",
    more: List[(String, String)] = Nil
) {

  def frag = scalatags.Text.RawFrag(s"${og.str}${twitter.str}")

  object og {

    private def tag(name: String, value: String) =
      s"""<meta property="og:$name" content="${escapeHtml(value)}"/>"""

    private val tupledTag = (tag _).tupled

    def str = List(
      "title" -> title,
      "description" -> description,
      "url" -> url,
      "type" -> `type`,
      "site_name" -> siteName
    ).map(tupledTag).mkString +
      image.?? { tag("image", _) } +
      more.map(tupledTag).mkString
  }

  object twitter {

    private def tag(name: String, value: String) =
      s"""<meta name="twitter:$name" content="${escapeHtml(value)}"/>"""

    private val tupledTag = (tag _).tupled

    def str = List(
      "card" -> "summary",
      "title" -> title,
      "description" -> description
    ).map(tupledTag).mkString +
      image.?? { tag("image", _) } +
      more.map(tupledTag).mkString
  }
}
