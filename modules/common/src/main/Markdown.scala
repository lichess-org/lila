package lila.common

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import java.util.Arrays
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

  private type Key  = String
  private type Text = String
  private type Html = String

  private val extensions = new java.util.ArrayList[com.vladsch.flexmark.util.misc.Extension]()
  if (table) extensions.add(TablesExtension.create())
  if (strikeThrough) extensions.add(StrikethroughExtension.create())
  if (autoLink) extensions.add(AutolinkExtension.create())

  private val options = new MutableDataSet()
    .set(Parser.EXTENSIONS, extensions)
    .set(HtmlRenderer.ESCAPE_HTML, Boolean box true)
    .set(HtmlRenderer.SOFT_BREAK, "<br>")
    // always disabled
    .set(Parser.HTML_BLOCK_PARSER, Boolean box false)
    .set(Parser.INDENTED_CODE_BLOCK_PARSER, Boolean box false)
    .set(Parser.FENCED_CODE_BLOCK_PARSER, Boolean box code)

  // configurable
  if (table) options.set(TablesExtension.CLASS_NAME, "slist")
  if (!header) options.set(Parser.HEADING_PARSER, Boolean box false)
  if (!blockQuote) options.set(Parser.BLOCK_QUOTE_PARSER, Boolean box false)
  if (!list) options.set(Parser.LIST_BLOCK_PARSER, Boolean box false)

  private val immutableOptions = options.toImmutable

  private val parser   = Parser.builder(immutableOptions).build()
  private val renderer = HtmlRenderer.builder(immutableOptions).build()

  private val logger = lila.log("markdown")

  // quick and dirty.
  // there should be a clean way to do it:
  // https://programming.vip/docs/flexmark-java-markdown-add-target-attribute-to-link.html
  private def addLinkAttributes(markup: String) =
    markup.replace("<a href=", """<a rel="nofollow noopener noreferrer" href=""")

  def apply(key: Key)(text: Text): Html =
    Chronometer
      .sync {
        try {
          addLinkAttributes(renderer.render(parser.parse(text)))
        } catch {
          case e: StackOverflowError =>
            logger.branch(key).error("StackOverflowError", e)
            text
        }
      }
      .mon(_.markdown.time)
      .logIfSlow(100, logger.branch(key))(_ => s"slow markdown size:${text.size}")
      .result
}

object Markdown {

  private val imageRegex = """!\[[^\]]*\]\((.*?)\s*("(?:.*[^"])")?\s*\)""".r

  def imageUrls(markdown: String): List[String] =
    imageRegex.findAllIn(markdown).matchData.map(_ group 1).toList
}
