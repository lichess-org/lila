package lila.app
package ui

import lila.app.ui.ScalatagsTemplate._
import play.api.i18n.Lang

case class OpenGraph(
    title: String,
    description: String,
    url: String,
    `type`: String = "website",
    image: Option[String] = None,
    twitterImage: Option[String] = None,
    more: List[(String, String)] = Nil
) {

  def frags(implicit lang: Lang): List[Frag] = og.frags ::: twitter.frags

  object og {

    private val property = attr("property")

    private def tag(name: String, value: String) =
      meta(
        property := s"og:$name",
        content  := value
      )

    private val tupledTag = (tag _).tupled

    def frags(implicit lang: Lang): List[Frag] =
      List(
        "title"       -> title,
        "description" -> description,
        "url"         -> url,
        "type"        -> `type`,
        "locale"      -> lang.language,
        "site_name"   -> "lishogi.org"
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
