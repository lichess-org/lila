package lila.common

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.{ TablesExtension, TableBlock }
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
import scala.collection.Set
import scala.jdk.CollectionConverters.*
import scala.util.Try
import com.vladsch.flexmark.util.misc.Extension
import lila.base.RawHtml
import com.vladsch.flexmark.html.renderer.ResolvedLink
import chess.format.pgn.PgnStr
import lila.common.config.AssetDomain

final class MarkdownRender(
    autoLink: Boolean = true,
    table: Boolean = false,
    strikeThrough: Boolean = false,
    header: Boolean = false,
    blockQuote: Boolean = false,
    list: Boolean = false,
    code: Boolean = false,
    pgnExpand: Option[MarkdownRender.PgnSourceExpand] = None,
    assetDomain: Option[AssetDomain] = None
):

  private val extensions = java.util.ArrayList[Extension]()
  if (table)
    extensions.add(TablesExtension.create())
    extensions.add(MarkdownRender.tableWrapperExtension)
  if (strikeThrough) extensions.add(StrikethroughExtension.create())
  if (autoLink)
    extensions.add(AutolinkExtension.create())
    extensions.add(MarkdownRender.WhitelistedImage.create(assetDomain))
  extensions.add(
    pgnExpand.fold[Extension](MarkdownRender.LilaLinkExtension) { MarkdownRender.PgnEmbedExtension(_) }
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

  type Key         = String
  type PgnSourceId = String

  case class PgnSourceExpand(domain: config.NetDomain, getPgn: PgnSourceId => Option[PgnStr])

  private val rel = "nofollow noopener noreferrer"

  private object WhitelistedImage:

    private val whitelist = AssetDomain.from:
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
        "images.prismic.io"
      )

    private def whitelistedSrc(src: String, assetDomain: Option[AssetDomain]): Option[String] = for
      url <- Try(URL.parse(src)).toOption
      if url.scheme == "http" || url.scheme == "https"
      host <- Option(url.host).map(_.toHostString)
      if (assetDomain.toList ::: whitelist).exists(h => host == h.value || host.endsWith(s".$h"))
    yield url.toString

    def create(assetDomain: Option[AssetDomain]) = new HtmlRenderer.HtmlRendererExtension:
      override def rendererOptions(options: MutableDataHolder) = ()
      override def extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) =
        htmlRendererBuilder
          .nodeRendererFactory(new NodeRendererFactory {
            override def apply(options: DataHolder) = new NodeRenderer:
              override def getNodeRenderingHandlers() =
                Set(NodeRenderingHandler(classOf[Image], render _)).asJava
          })

      private def render(node: Image, context: NodeRendererContext, html: HtmlWriter): Unit =
        // Based on implementation in CoreNodeRenderer.
        if (context.isDoNotRenderLinks || CoreNodeRenderer.isSuppressedLinkPrefix(node.getUrl(), context))
          context.renderChildren(node)
        else
          {
            val resolvedLink = context.resolveLink(LinkType.IMAGE, node.getUrl().unescape(), null, null)
            val url          = resolvedLink.getUrl()
            val altText      = new TextCollectingVisitor().collectAndGetText(node)
            whitelistedSrc(url, assetDomain) match
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

  private class PgnEmbedExtension(expander: PgnSourceExpand) extends HtmlRenderer.HtmlRendererExtension:
    override def rendererOptions(options: MutableDataHolder) = ()
    override def extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) =
      htmlRendererBuilder
        .nodeRendererFactory(new NodeRendererFactory {
          override def apply(options: DataHolder) = new PgnEmbedNodeRenderer(expander)
        })
        .unit
  private class PgnEmbedNodeRenderer(expander: PgnSourceExpand) extends NodeRenderer:
    override def getNodeRenderingHandlers() =
      new java.util.HashSet(
        Arrays.asList(
          new NodeRenderingHandler(classOf[Link], renderLink _),
          new NodeRenderingHandler(classOf[AutoLink], renderAutoLink _)
        )
      )

    private val gameRegex =
      s"""^(?:https?://)?${expander.domain}/(?:embed/)?(?:game/)?(\\w{8})(?:(?:/(white|black))|\\w{4}|)(?:#(\\d+))?$$""".r
    private val chapterRegex =
      s"""^(?:https?://)?${expander.domain}/study/(?:embed/)?(?:\\w{8})/(\\w{8})(?:#(last|\\d+))?$$""".r

    private def renderLink(node: Link, context: NodeRendererContext, html: HtmlWriter): Unit =
      renderLinkNode(node, context, html)

    private def renderAutoLink(node: AutoLink, context: NodeRendererContext, html: HtmlWriter): Unit =
      renderLinkNode(node, context, html)

    private def renderLinkNode(node: LinkNode, context: NodeRendererContext, html: HtmlWriter) =
      // Based on implementation in CoreNodeRenderer.
      if (context.isDoNotRenderLinks || CoreNodeRenderer.isSuppressedLinkPrefix(node.getUrl(), context))
        context.renderChildren(node)
      else
        val link         = context.resolveLink(LinkType.LINK, node.getUrl().unescape(), null, null)
        def justAsLink() = renderLinkWithBase(node, context, html, link)
        link.getUrl match
          case gameRegex(id, color, ply) =>
            expander
              .getPgn(id)
              .fold(justAsLink())(renderPgnViewer(node, html, link, _, Option(color), Option(ply)))
          case chapterRegex(id, ply) =>
            expander
              .getPgn(id)
              .fold(justAsLink())(renderPgnViewer(node, html, link, _, none, Option(ply)))
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
        color: Option[String],
        ply: Option[String]
    ) =
      html
        .attr("data-pgn", pgn.value)
        .attr("class", "lpv--autostart is2d")
      color.foreach:
        html.attr("data-orientation", _)
      ply.foreach:
        html.attr("data-ply", _)
      html
        .srcPos(node.getChars())
        .withAttr(link)
        .tag("div")
        .text(link.getUrl)
        .tag("/div")

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

  private val tableWrapperExtension = new HtmlRenderer.HtmlRendererExtension:
    override def rendererOptions(options: MutableDataHolder) = ()
    override def extend(builder: HtmlRenderer.Builder, rendererType: String) = builder.nodeRendererFactory(
      new NodeRendererFactory:
        override def apply(options: DataHolder) = new NodeRenderer:
          override def getNodeRenderingHandlers() = Set(
            new NodeRenderingHandler(
              classOf[TableBlock],
              (node: TableBlock, context: NodeRendererContext, html: HtmlWriter) =>
                html.withAttr().attr("class", "slist-wrapper").tag("div")
                context.delegateRender();
                html.tag("/div")
            )
          ).asJava
    )
