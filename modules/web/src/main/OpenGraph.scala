package lila.web

import scalatags.text.Frag
import scalatags.Text.all.*

case class OpenGraph(
    title: String,
    description: String,
    url: String,
    `type`: String = "website",
    image: Option[String] = None,
    twitterImage: Option[String] = None,
    siteName: String = "lichess.org",
    more: List[(String, String)] = Nil
) extends scalatags.Text.Aggregate:

  def frags: List[Frag] = og.frags ::: twitter.frags

  object og:

    private val property = attr("property")

    private def tag(name: String, value: String) =
      meta(
        property := s"og:$name",
        content  := value
      )

    private val tupledTag = (tag).tupled

    def frags: List[Frag] =
      List(
        "title"       -> title,
        "description" -> description,
        "url"         -> url,
        "type"        -> `type`,
        "site_name"   -> siteName
      ).map(tupledTag) :::
        image.map { tag("image", _) }.toList :::
        more.map(tupledTag)

  object twitter:

    private def tag(n: String, value: String) =
      meta(
        name    := s"twitter:$n",
        content := value
      )

    private val tupledTag = (tag).tupled

    def frags: List[Frag] =
      List(
        "card"        -> "summary",
        "title"       -> title,
        "description" -> description
      ).map(tupledTag) :::
        (twitterImage.orElse(image)).map { tag("image", _) }.toList :::
        more.map(tupledTag)
