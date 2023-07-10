package lila.blog

import scala.util.matching.Regex

import lila.common.{ Markdown, MarkdownRender }

object BlogTransform:

  private val RemoveRegex        = """http://(\w{2}\.)?+lichess\.org""".r
  def removeProtocol(html: Html) = Html(RemoveRegex.replaceAllIn(html.value, _ => "//lichess.org"))

  private val AddRegex        = """(https?+:)?+(//)?+(\w{2}\.)?+lichess\.org""".r
  def addProtocol(html: Html) = Html(AddRegex.replaceAllIn(html.value, _ => "https://lichess.org"))

  object markdown:

    private val renderer = MarkdownRender(table = true)

    // hash code collisions can't be a vector of attack here,
    // since only lichess team members can write these blog posts
    private val cache = lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterAccess(20 minutes)
      .maximumSize(64)
      .build[Int, Html]()

    private val PreRegex = """<pre>markdown(.+)</pre>""".r

    def apply(html: Html): Html = Html:
      PreRegex.replaceAllIn(
        html.value,
        m =>
          val markdown = m group 1
          val markup = cache.get(
            markdown.hashCode,
            _ => renderer("prismic")(Markdown(markdown.replace("<br>", "\n")))
          )
          Regex.quoteReplacement(markup.value)
      )
