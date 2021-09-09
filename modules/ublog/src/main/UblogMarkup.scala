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

  def apply(post: UblogPost): String =
    cache.get(post.markdown, str => postProcess(renderer(s"ublog:${post.id}")(preProcess(str))))

  private def preProcess = replaceGameGifs.apply _

  private def postProcess = imageParagraph.apply _

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20 minutes)
    .maximumSize(1024)
    .build[String, String]()

  private object replaceGameGifs {
    val regex = {
      """!\[[^\]]*\]\(""" + net.assetBaseUrl + """/game/export/gif/(white|black)/(\w{8}).gif\)"""
    }.r
    def apply(markdown: String) = regex.replaceAllIn(markdown, net.baseUrl.value + "/$2/$1")
  }

  private object imageParagraph {
    def apply(markup: String) = markup.replace("""<p><img src=""", """<p class="img-container"><img src=""")
  }
}
