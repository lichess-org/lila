package lila.app
package ui

import play.twirl.api.Html

case class OpenGraph(
    title: String,
    description: String,
    url: String,
    `type`: String = "website",
    image: Option[String] = None,
    siteName: String = "lichess.org") {

  def html = Html(toString)

  private def prop(name: String, value: String) =
    s"""<meta property="og:$name" content="$value" />"""

  override def toString = List(
    "title" -> title,
    "description" -> description,
    "url" -> url,
    "type" -> `type`,
    "siteName" -> siteName
  ).map((prop _).tupled).mkString + image.?? { prop("image", _) }
}
