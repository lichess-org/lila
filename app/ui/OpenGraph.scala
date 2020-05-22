package lila.app
package ui

import lila.app.ui.ScalatagsTemplate._

case class OpenGraph(
    title: String,
    description: String,
    url: String,
    `type`: String = "website",
    image: Option[String] = None,
    twitterImage: Option[String] = None,
    siteName: String = "lichess.org",
    more: List[(String, String)] = Nil
) {

  def frags: List[Frag] = og.frags ::: twitter.frags

  object og {

    private val property = attr("property")

    private def tag(name: String, value: String) =
      meta(
        property := s"og:$name",
        content := value
      )

    private val tupledTag = (tag _).tupled

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
  }

  object twitter {

    private def tag(name: String, value: String) =
      meta(
        st.name := s"twitter:$name",
        content := value
      )

    private val tupledTag = (tag _).tupled

    def frags: List[Frag] =
      List(
        "card"        -> "summary",
        "title"       -> title,
        "description" -> description
      ).map(tupledTag) :::
        (twitterImage orElse image).map { tag("image", _) }.toList :::
        more.map(tupledTag)
  }
}
