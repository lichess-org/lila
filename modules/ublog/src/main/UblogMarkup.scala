package lila.ublog

import java.util.regex.Matcher
import scala.concurrent.duration._

import lila.common.config
import lila.common.{ Chronometer, Markdown, MarkdownRender }
import lila.memo.CacheApi
import play.api.Mode

final class UblogMarkup(
    gameExpand: lila.game.GameTextExpand,
    baseUrl: config.BaseUrl,
    assetBaseUrl: config.AssetBaseUrl,
    cacheApi: CacheApi,
    netDomain: config.NetDomain
)(implicit ec: scala.concurrent.ExecutionContext, mode: Mode) {

  private val renderer = new MarkdownRender(
    autoLink = true,
    list = true,
    strikeThrough = true,
    header = true,
    blockQuote = true,
    code = true,
    table = true,
    gameExpand = MarkdownRender.GameExpand(netDomain, gameExpand.getPgnSync).some
  )

  def apply(post: UblogPost) = cache.get((post.id, post.markdown)).map(scalatags.Text.all.raw)

  private type Html = String

  private val cache = cacheApi[(UblogPost.Id, Markdown), Html](2048, "ublog.markup") {
    _.maximumSize(2048)
      .expireAfterWrite(if (mode == Mode.Prod) 15 minutes else 1 second)
      .buildAsyncFuture { case (id, markdown) =>
        // todo game preload?
        val res = process(id)(markdown)
        fuccess(res)
      }
  }

  private def process(id: UblogPost.Id): Markdown => Html = replaceGameGifs.apply andThen
    unescapeAtUsername.apply andThen
    renderer(s"ublog:${id}") andThen
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
