package lila.clas

import com.github.blemale.scaffeine.Cache
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import java.util.Arrays
import scala.concurrent.duration._

final class ClasMarkup {

  type Text = String
  type Html = String

  private val options = new MutableDataSet()
  options.set(
    Parser.EXTENSIONS,
    Arrays.asList(
      TablesExtension.create(),
      StrikethroughExtension.create(),
      AutolinkExtension.create()
    )
  )
  options.set(HtmlRenderer.SOFT_BREAK, "<br>\n")
  options.set(TablesExtension.CLASS_NAME, "slist")
  private val parser   = Parser.builder(options).build()
  private val renderer = HtmlRenderer.builder(options).build()

  private val cache: Cache[Text, Html] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(10 minutes)
    .maximumSize(64)
    .build[Text, Html]

  private def compute(text: Text): Html =
    renderer.render(parser.parse(text))

  def apply(text: Text): Html = cache.get(text, compute)
}
