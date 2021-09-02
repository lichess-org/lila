package lila.blog

import scala.concurrent.duration._
import scala.util.matching.Regex

object BlogTransform {

  private val RemoveRegex          = """http://(\w{2}\.)?+lichess\.org""".r
  def removeProtocol(html: String) = RemoveRegex.replaceAllIn(html, _ => "//lichess.org")

  private val AddRegex          = """(https?+:)?+(//)?+(\w{2}\.)?+lichess\.org""".r
  def addProtocol(html: String) = AddRegex.replaceAllIn(html, _ => "https://lichess.org")

  object markdown {
    private type Text = String
    private type Html = String

    private val renderer = new lila.common.Markdown(table = true)

    // hash code collisions can't be a vector of attack here,
    // since only lichess team members can write these blog posts
    private val cache = lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterAccess(20 minutes)
      .maximumSize(64)
      .build[Int, String]()

    private val PreRegex = """<pre>markdown(.+)</pre>""".r

    def apply(html: Html): Html =
      PreRegex.replaceAllIn(
        html,
        m => {
          val markdown = m group 1
          val markup =
            cache.get(markdown.hashCode, _ => renderer("prismic")(markdown.replace("<br>", "\n")))
          Regex.quoteReplacement(markup)
        }
      )

  }
}
