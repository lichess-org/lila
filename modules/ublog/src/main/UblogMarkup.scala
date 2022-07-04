package lila.ublog

import java.util.regex.Matcher
import scala.concurrent.duration._

import lila.common.{ Chronometer, Markdown, MarkdownRender }
import lila.common.config
import lila.common.config.NetConfig

final class UblogMarkup(baseUrl: config.BaseUrl, assetBaseUrl: config.AssetBaseUrl) {

  private val renderer = new MarkdownRender(
    autoLink = true,
    list = true,
    strikeThrough = true,
    header = true,
    blockQuote = true,
    code = true,
    table = true
  )

  type Html = String

  def apply(post: UblogPost): Html = cache.get(post.markdown, process(post))

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler.maximumSize(2048).build[Markdown, Html]()

  private def process(post: UblogPost): Markdown => Html = replaceGameGifs.apply andThen
    unescapeAtUsername.apply andThen
    renderer(s"ublog:${post.id}") andThen
    imageParagraph andThen
    unescapeUnderscoreInLinks.apply

  // replace game GIFs URLs with actual game URLs that can be embedded
  private object replaceGameGifs {
    private val regex = (assetBaseUrl.value + """/game/export/gif(/white|/black|)/(\w{8})\.gif""").r
    val apply         = (markdown: Markdown) => markdown(m => regex.replaceAllIn(m, baseUrl.value + "/$2$1"))
  }

  // put images into a container for styling
  private def imageParagraph(markup: Html) =
    markup.replace("""<p><img src=""", """<p class="img-container"><img src=""")

  private def unescape(txt: String) = txt.replace("""\_""", "_")

  // https://github.com/lichess-org/lila/issues/9767
  // toastui editor escapes `_` as `\_` and it breaks autolinks
  private[ublog] object unescapeUnderscoreInLinks {
    private val hrefRegex    = """href="([^"]+)"""".r
    private val contentRegex = """>([^<]+)</a>""".r
    def apply(markup: Html) = contentRegex.replaceAllIn(
      hrefRegex.replaceAllIn(markup, m => s"""href="${Matcher.quoteReplacement(unescape(m group 1))}""""),
      m => s""">${Matcher.quoteReplacement(unescape(m group 1))}</a>"""
    )
  }

  // toastui editor escapes `_` as `\_` and it breaks @username
  private[ublog] object unescapeAtUsername {
    // Same as `atUsernameRegex` in `RawHtmlTest.scala` but it also matchs the '\' character.
    // Can't end with '\', which would be escaping something after the username, like '\)'
    private val atUsernameRegexEscaped = """@(?<![\w@#/]@)([\w\\-]{1,29}\w)(?![@\w-]|\.\w)""".r
    def apply(markdown: Markdown) =
      markdown(m => atUsernameRegexEscaped.replaceAllIn(m, a => s"@${unescape(a group 1)}"))
  }
}
