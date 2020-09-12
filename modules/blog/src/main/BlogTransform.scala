package lila.blog

import com.github.blemale.scaffeine.LoadingCache
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

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

    private val PreRegex = """<pre>markdown(.+)</pre>""".r

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
      .expireAfterAccess(15 minutes)
      .maximumSize(32)
      .build((text: Text) => renderer.render(parser.parse(text.replace("<br>", "\n"))))

    def apply(html: Html): Html =
      PreRegex.replaceAllIn(html, m => Regex.quoteReplacement(cache get m.group(1)))
  }
}
