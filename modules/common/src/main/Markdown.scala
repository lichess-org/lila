package lila.common

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

final class Markdown(autoLink: Boolean = false) {

  private type Text = String
  private type Html = String

  private val extensions = new java.util.ArrayList[com.vladsch.flexmark.util.misc.Extension]()
  extensions.add(TablesExtension.create())
  extensions.add(StrikethroughExtension.create())
  if (autoLink) extensions.add(AutolinkExtension.create())

  private val options = new MutableDataSet()
    .set(Parser.EXTENSIONS, extensions)
    .set(HtmlRenderer.ESCAPE_HTML, Boolean box true)
    .set(HtmlRenderer.SOFT_BREAK, "<br>")
    // always disabled
    .set(Parser.HTML_BLOCK_PARSER, Boolean box false)
    .set(Parser.INDENTED_CODE_BLOCK_PARSER, Boolean box false)

  private val immutableOptions = options.toImmutable

  private val parser   = Parser.builder(immutableOptions).build()
  private val renderer = HtmlRenderer.builder(immutableOptions).build()

  def apply(text: Text): Html = try {
    renderer.render(parser.parse(text))
  } catch {
    case e: StackOverflowError =>
      lila.log("markdown").error("StackOverflowError", e)
      text
  }
}
