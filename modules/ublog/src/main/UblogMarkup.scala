package lila.ublog

import scala.concurrent.duration._
import lila.common.Chronometer
import lila.common.config.NetConfig

final class UblogMarkup(net: NetConfig) {

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

  private def postProcess(html: String) = imageParagraph(html)

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .maximumSize(2048)
    .build[Text, Html]()

  // replace game GIFs URLs with actual game URLs that can be embedded
  private object replaceGameGifs {
    val regex                 = (net.assetBaseUrl + """/game/export/gif(/white|/black|)/(\w{8})\.gif""").r
    def apply(markdown: Text) = regex.replaceAllIn(markdown, net.baseUrl.value + "/$2$1")
  }

  // put images into a container for styling
  private def imageParagraph(markup: Html) =
    markup.replace("""<p><img src=""", """<p class="img-container"><img src=""")
}
