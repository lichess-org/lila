package lila.relay

import com.github.blemale.scaffeine.LoadingCache
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

import scala.concurrent.duration._

final private class RelayMarkup {

  private type Text = String
  private type Html = String

  private val options = new MutableDataSet()
  options.set(
    Parser.EXTENSIONS,
    java.util.Arrays.asList(TablesExtension.create(), StrikethroughExtension.create())
  )
  options.set(HtmlRenderer.SOFT_BREAK, "<br>\n")
  options.set(TablesExtension.CLASS_NAME, "slist")
  private val parser   = Parser.builder(options).build()
  private val renderer = HtmlRenderer.builder(options).build()

  private val cache: LoadingCache[Text, Html] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(10 minutes)
    .maximumSize(64)
    .build((t: Text) => renderer.render(parser.parse(t)))

  def apply(text: Text): Html = cache.get(text)
}
