package lila.ublog

import scala.concurrent.duration._

import lila.common.Chronometer
import lila.common.config
import lila.common.config.NetConfig

final class UblogMarkup(baseUrl: config.BaseUrl, assetBaseUrl: config.AssetBaseUrl) {

  private val renderer =
    new lila.common.Markdown(
      autoLink = true,
      list = true,
      strikeThrough = true,
      header = true,
      blockQuote = true,
      code = true,
      table = true
    )

  type Text = String
  type Html = String

  def apply(post: UblogPost): String =
    cache.get(post.markdown, str => postProcess(renderer(s"ublog:${post.id}")(preProcess(str))))

  private def preProcess = replaceGameGifs.apply _

  private def postProcess(html: String) = unescapeUnderscoreInLinks(imageParagraph(html))

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .maximumSize(2048)
    .build[Text, Html]()

  // replace game GIFs URLs with actual game URLs that can be embedded
  private object replaceGameGifs {
    private val regex         = (assetBaseUrl.value + """/game/export/gif(/white|/black|)/(\w{8})\.gif""").r
    def apply(markdown: Text) = regex.replaceAllIn(markdown, baseUrl.value + "/$2$1")
  }

  // put images into a container for styling
  private def imageParagraph(markup: Html) =
    markup.replace("""<p><img src=""", """<p class="img-container"><img src=""")

  // https://github.com/ornicar/lila/issues/9767
  // toastui editor escapes `_` as `\_` and it breaks autolinks
  private[ublog] object unescapeUnderscoreInLinks {
    private val hrefRegex             = """href="([^"]+)"""".r
    private val contentRegex          = """>([^<]+)</a>""".r
    private def unescape(txt: String) = txt.replace("""\\_""", "_")
    def apply(markup: Html) = contentRegex.replaceAllIn(
      hrefRegex.replaceAllIn(markup, m => s"""href="${unescape(m group 1)}""""),
      m => s""">${unescape(m group 1)}</a>"""
    )
  }
}
