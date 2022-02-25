package lila.common

import lila.base.RawHtml
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.{
  AttributeProvider,
  HtmlRenderer,
  HtmlWriter,
  IndependentAttributeProviderFactory
}
import com.vladsch.flexmark.html.renderer.{
  AttributablePart,
  CoreNodeRenderer,
  LinkResolverContext,
  LinkType,
  NodeRenderer,
  NodeRendererContext,
  NodeRendererFactory,
  NodeRenderingHandler
}
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.{ Node, TextCollectingVisitor }
import com.vladsch.flexmark.util.data.{ DataHolder, MutableDataHolder, MutableDataSet }
import com.vladsch.flexmark.util.html.MutableAttributes
import com.vladsch.flexmark.ast.{ Image, Link }
import io.mola.galimatias.URL
import scala.collection.JavaConverters
import java.util.Arrays
import scala.jdk.CollectionConverters._
import scala.util.Try

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
  extensions.add(Markdown.NofollowExtension)
  extensions.add(Markdown.WhitelistedImageExtension)

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

  private def mentionsToLinks(markdown: Text): Text =
    RawHtml.atUsernameRegex.replaceAllIn(markdown, "[@$1](/@/$1)")

  // https://github.com/vsch/flexmark-java/issues/496
  private val tooManyUnderscoreRegex             = """(_{4,})""".r
  private def preventStackOverflow(text: String) = tooManyUnderscoreRegex.replaceAllIn(text, "_" * 3)

  def apply(key: Key)(text: Text): Html =
    Chronometer
      .sync {
        try {
          renderer.render(parser.parse(mentionsToLinks(preventStackOverflow(text))))
        } catch {
          case e: StackOverflowError =>
            logger.branch(key).error("StackOverflowError", e)
            text
        }
      }
      .mon(_.markdown.time)
      .logIfSlow(50, logger.branch(key))(_ => s"slow markdown size:${text.size}")
      .result
}

object Markdown {

  private val rel = "nofollow noopener noreferrer"

  private object WhitelistedImageExtension extends HtmlRenderer.HtmlRendererExtension {
    override def rendererOptions(options: MutableDataHolder) = ()
    override def extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) =
      htmlRendererBuilder
        .nodeRendererFactory(new NodeRendererFactory {
          override def apply(options: DataHolder) = WhitelistedImageNodeRenderer
        })
        .unit
  }
  private object WhitelistedImageNodeRenderer extends NodeRenderer {
    override def getNodeRenderingHandlers() =
      new java.util.HashSet(
        Arrays.asList(
          new NodeRenderingHandler(classOf[Image], render)
        )
      )

    private val whitelist =
      List(
        "imgur.com",
        "giphy.com",
        "wikimedia.org",
        "creativecommons.org",
        "pexels.com",
        "piqsels.com",
        "freeimages.com",
        "unsplash.com",
        "pixabay.com",
        "githubusercontent.com",
        "googleusercontent.com",
        "i.ibb.co",
        "i.postimg.cc",
        "xkcd.com",
        "lichess1.org"
      )
    private def whitelistedSrc(src: String): Option[String] =
      for {
        url <- Try(URL.parse(src)).toOption
        if url.scheme == "http" || url.scheme == "https"
        host <- Option(url.host).map(_.toString)
        if whitelist.exists(h => host == h || host.endsWith(s".$h"))
      } yield url.toString

    private def render(node: Image, context: NodeRendererContext, html: HtmlWriter): Unit =
      // Based on implementation in CoreNodeRenderer.
      if (context.isDoNotRenderLinks || CoreNodeRenderer.isSuppressedLinkPrefix(node.getUrl(), context))
        context.renderChildren(node)
      else {
        val resolvedLink = context.resolveLink(LinkType.IMAGE, node.getUrl().unescape(), null, null)
        val url          = resolvedLink.getUrl()
        val altText      = new TextCollectingVisitor().collectAndGetText(node)
        whitelistedSrc(url) match {
          case Some(src) =>
            html
              .srcPos(node.getChars())
              .attr("src", src)
              .attr("alt", altText)
              .attr(resolvedLink.getNonNullAttributes())
              .withAttr(resolvedLink)
              .tagVoid("img")
          case None =>
            html
              .srcPos(node.getChars())
              .attr("href", url)
              .attr("rel", rel)
              .withAttr(resolvedLink)
              .tag("a")
              .text(altText)
              .tag("/a")
        }
      }.unit
  }

  private object NofollowExtension extends HtmlRenderer.HtmlRendererExtension {
    override def rendererOptions(options: MutableDataHolder) = ()
    override def extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) =
      htmlRendererBuilder
        .attributeProviderFactory(new IndependentAttributeProviderFactory {
          override def apply(context: LinkResolverContext): AttributeProvider = NofollowAttributeProvider
        })
        .unit
  }
  private object NofollowAttributeProvider extends AttributeProvider {
    override def setAttributes(node: Node, part: AttributablePart, attributes: MutableAttributes) = {
      if (node.isInstanceOf[Link] && part == AttributablePart.LINK)
        attributes.replaceValue("rel", rel).unit
    }
  }
}
