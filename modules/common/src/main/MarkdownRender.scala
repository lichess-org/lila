package lila.common

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
import com.vladsch.flexmark.ast.{ AutoLink, Image, Link, LinkNode }
import io.mola.galimatias.URL
import java.util.Arrays
import scala.jdk.CollectionConverters.*
import scala.util.Try
import com.vladsch.flexmark.util.misc.Extension
import lila.base.RawHtml
import com.vladsch.flexmark.html.renderer.ResolvedLink
import chess.format.pgn.PgnStr

final class MarkdownRender(
    autoLink: Boolean = true,
    table: Boolean = false,
    strikeThrough: Boolean = false,
    header: Boolean = false,
    blockQuote: Boolean = false,
    list: Boolean = false,
    code: Boolean = false,
    gameExpand: Option[MarkdownRender.GameExpand] = None
):

  private val extensions = java.util.ArrayList[Extension]()
  if (table) extensions.add(TablesExtension.create())
  if (strikeThrough) extensions.add(StrikethroughExtension.create())
  if (autoLink)
    extensions.add(AutolinkExtension.create())
    extensions.add(MarkdownRender.WhitelistedImage.extension)
  extensions.add(
    gameExpand.fold[Extension](MarkdownRender.LilaLinkExtension) { MarkdownRender.GameEmbedExtension(_) }
  )

  private val options = MutableDataSet()
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

  private def mentionsToLinks(markdown: Markdown): Markdown =
    Markdown(RawHtml.atUsernameRegex.replaceAllIn(markdown.value, "[@$1](/@/$1)"))

  // https://github.com/vsch/flexmark-java/issues/496
  private val tooManyUnderscoreRegex = """(_{4,})""".r
  private def preventStackOverflow(text: Markdown) = Markdown(
    tooManyUnderscoreRegex.replaceAllIn(text.value, "_" * 3)
  )

  def apply(key: MarkdownRender.Key)(text: Markdown): Html = Html {
    Chronometer
      .sync {
        try renderer.render(parser.parse(mentionsToLinks(preventStackOverflow(text)).value))
        catch
          case e: StackOverflowError =>
            logger.branch(key).error("StackOverflowError", e)
            text.value
      }
      .mon(_.markdown.time)
      .logIfSlow(50, logger.branch(key))(_ => s"slow markdown size:${text.value.size}")
      .result
  }

object MarkdownRender:

  type Key = String

  case class GameExpand(domain: config.NetDomain, getPgn: GameId => Option[PgnStr])

  private val rel = "nofollow noopener noreferrer"

  private object WhitelistedImage:

    val extension = new HtmlRenderer.HtmlRendererExtension:
      override def rendererOptions(options: MutableDataHolder) = ()
      override def extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) =
        htmlRendererBuilder
          .nodeRendererFactory(new NodeRendererFactory {
            override def apply(options: DataHolder) = renderer
          })

    private val renderer = new NodeRenderer:
      override def getNodeRenderingHandlers() =
        java.util.HashSet(Arrays.asList(NodeRenderingHandler(classOf[Image], render _)))

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
    private def whitelistedSrc(src: String): Option[String] = for
      url <- Try(URL.parse(src)).toOption
      if url.scheme == "http" || url.scheme == "https"
      host <- Option(url.host).map(_.toHostString)
      if whitelist.exists(h => host == h || host.endsWith(s".$h"))
    yield url.toString

    private def render(node: Image, context: NodeRendererContext, html: HtmlWriter): Unit =
      // Based on implementation in CoreNodeRenderer.
      if (context.isDoNotRenderLinks || CoreNodeRenderer.isSuppressedLinkPrefix(node.getUrl(), context))
        context.renderChildren(node)
      else
        {
          val resolvedLink = context.resolveLink(LinkType.IMAGE, node.getUrl().unescape(), null, null)
          val url          = resolvedLink.getUrl()
          val altText      = new TextCollectingVisitor().collectAndGetText(node)
          whitelistedSrc(url) match
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
        }.unit

  private class GameEmbedExtension(expander: GameExpand) extends HtmlRenderer.HtmlRendererExtension:
    override def rendererOptions(options: MutableDataHolder) = ()
    override def extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) =
      htmlRendererBuilder
        .nodeRendererFactory(new NodeRendererFactory {
          override def apply(options: DataHolder) = new GameEmbedNodeRenderer(expander)
        })
        .unit
  private class GameEmbedNodeRenderer(expander: GameExpand) extends NodeRenderer:
    override def getNodeRenderingHandlers() =
      new java.util.HashSet(
        Arrays.asList(
          new NodeRenderingHandler(classOf[Link], renderLink _),
          new NodeRenderingHandler(classOf[AutoLink], renderAutoLink _)
        )
      )

    private val gameRegex =
      s"""^(?:https?://)?${expander.domain}/(?:embed/)?(?:game/)?(\\w{8})(?:(?:/(white|black))|\\w{4}|)(?:#(\\d+))?$$""".r

    private def renderLink(node: Link, context: NodeRendererContext, html: HtmlWriter): Unit =
      // Based on implementation in CoreNodeRenderer.
      if (context.isDoNotRenderLinks || CoreNodeRenderer.isSuppressedLinkPrefix(node.getUrl(), context))
        context.renderChildren(node)
      else
        val link         = context.resolveLink(LinkType.LINK, node.getUrl().unescape(), null, null)
        def justAsLink() = renderLinkWithBase(node, context, html, link)
        link.getUrl match
          case gameRegex(id, color, ply) =>
            expander.getPgn(GameId(id)).fold(justAsLink())(renderPgnViewer(node, html, link, _, color, ply))
          case _ => justAsLink()

    private def renderAutoLink(
        node: AutoLink,
        context: NodeRendererContext,
        html: HtmlWriter
    ): Unit =
      // Based on implementation in CoreNodeRenderer.
      if (context.isDoNotRenderLinks || CoreNodeRenderer.isSuppressedLinkPrefix(node.getUrl(), context))
        context.renderChildren(node)
      else
        val link         = context.resolveLink(LinkType.LINK, node.getUrl().unescape(), null, null)
        def justAsLink() = renderLinkWithBase(node, context, html, link)
        link.getUrl match
          case gameRegex(id, color, ply) =>
            expander.getPgn(GameId(id)).fold(justAsLink())(renderPgnViewer(node, html, link, _, color, ply))
          case _ => justAsLink()

    private def renderLinkWithBase(
        node: LinkNode,
        context: NodeRendererContext,
        html: HtmlWriter,
        baseLink: ResolvedLink
    ) =
      val link = if (node.getTitle.isNotNull) baseLink.withTitle(node.getTitle().unescape()) else baseLink
      html.attr("href", link.getUrl)
      html.attr(link.getNonNullAttributes())
      html.srcPos(node.getChars()).withAttr(link).tag("a")
      context.renderChildren(node)
      html.tag("/a").unit

    private def renderPgnViewer(
        node: LinkNode,
        html: HtmlWriter,
        link: ResolvedLink,
        pgn: PgnStr,
        color: String,
        ply: String
    ) =
      html
        .attr("data-pgn", pgn.value)
        .attr("data-orientation", Option(color) | "white")
        .attr("data-ply", Option(ply) | "")
        .attr("class", "lpv--autostart")
        .srcPos(node.getChars())
        .withAttr(link)
        .tag("div")
        .text(link.getUrl)
        .tag("/div")
        .unit

  private object LilaLinkExtension extends HtmlRenderer.HtmlRendererExtension:
    override def rendererOptions(options: MutableDataHolder) = ()
    override def extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) =
      htmlRendererBuilder
        .attributeProviderFactory(new IndependentAttributeProviderFactory {
          override def apply(context: LinkResolverContext): AttributeProvider = lilaLinkAttributeProvider
        })
        .unit

  private val lilaLinkAttributeProvider = new AttributeProvider:
    override def setAttributes(node: Node, part: AttributablePart, attributes: MutableAttributes) =
      if ((node.isInstanceOf[Link] || node.isInstanceOf[AutoLink]) && part == AttributablePart.LINK)
        attributes.replaceValue("rel", rel).unit
        attributes.replaceValue("href", RawHtml.removeUrlTrackingParameters(attributes.getValue("href"))).unit
