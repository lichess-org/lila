package lila.common

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import scala.jdk.CollectionConverters._

final class Markdown(
    autoLink: Boolean = true,
    table: Boolean = false,
    strikeThrough: Boolean = false,
    header: Boolean = false,
    blockQuote: Boolean = false,
    list: Boolean = false,
    code: Boolean = false
) {

  private type Text = String
  private type Html = String

  private val extensions: java.util.List[Parser.ParserExtension] = List(
    table option TablesExtension.create(),
    strikeThrough option StrikethroughExtension.create(),
    autoLink option AutolinkExtension.create()
  ).flatten.asJava

  private val options = new MutableDataSet()

  options.set(Parser.EXTENSIONS, extensions)
  options.set(HtmlRenderer.ESCAPE_HTML, Boolean box true)
  options.set(HtmlRenderer.SOFT_BREAK, "<br>")

  // always disabled
  options.set(Parser.HTML_BLOCK_PARSER, Boolean box false)
  options.set(Parser.INDENTED_CODE_BLOCK_PARSER, Boolean box false)

  options.set(Parser.FENCED_CODE_BLOCK_PARSER, Boolean box code)

  // configurable
  if (table) options.set(TablesExtension.CLASS_NAME, "slist")
  if (!header) options.set(Parser.HEADING_PARSER, Boolean box false)
  if (!blockQuote) options.set(Parser.BLOCK_QUOTE_PARSER, Boolean box false)
  if (!list) options.set(Parser.LIST_BLOCK_PARSER, Boolean box false)

  private val parser   = Parser.builder(options).build()
  private val renderer = HtmlRenderer.builder(options).build()

  def apply(text: Text): Html = renderer.render(parser.parse(text))
}
