package lila.common

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import scala.jdk.CollectionConverters._

final class Markdown(autoLink: Boolean = false) {

  private type Text = String
  private type Html = String

  private val extensions: java.util.List[Parser.ParserExtension] = List(
    TablesExtension.create().some,
    StrikethroughExtension.create().some,
    autoLink option AutolinkExtension.create()
  ).flatten.asJava

  private val options = new MutableDataSet()
  options.set(Parser.EXTENSIONS, extensions)
  options.set(TablesExtension.CLASS_NAME, "slist")
  options.set(HtmlRenderer.ESCAPE_HTML, Boolean.box(true))
  options.set(HtmlRenderer.SOFT_BREAK, "<br>")
  private val parser   = Parser.builder(options).build()
  private val renderer = HtmlRenderer.builder(options).build()

  def apply(text: Text): Html = renderer.render(parser.parse(text))
}
