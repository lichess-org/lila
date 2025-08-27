package lila.common

import chess.format.pgn.PgnStr
import com.vladsch.flexmark.ast.{ AutoLink, Image, Link, LinkNode }
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.{ TableBlock, TablesExtension }
import com.vladsch.flexmark.html.renderer.{
  AttributablePart,
  CoreNodeRenderer,
  LinkResolverContext,
  LinkType,
  NodeRenderer,
  NodeRendererContext,
  NodeRendererFactory,
  NodeRenderingHandler,
  ResolvedLink
}
import com.vladsch.flexmark.html.{
  AttributeProvider,
  HtmlRenderer,
  HtmlWriter,
  IndependentAttributeProviderFactory
}
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.{ Node, TextCollectingVisitor }
import com.vladsch.flexmark.util.data.{ DataHolder, MutableDataHolder, MutableDataSet }
import com.vladsch.flexmark.util.html.MutableAttributes
import com.vladsch.flexmark.util.misc.Extension

import java.util.Arrays
import scala.collection.Set
import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex

import lila.core.config.{ AssetDomain, NetDomain }
import lila.core.misc.lpv.LpvEmbed

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
  if header then extensions.add(AnchorLinkExtension.create())
  if table then
    extensions.add(TablesExtension.create())
    extensions.add(MarkdownRender.tableWrapperExtension)
  if strikeThrough then extensions.add(StrikethroughExtension.create())
  if autoLink then
    extensions.add(AutolinkExtension.create())
    extensions.add(MarkdownRender.WhitelistedImage.create(assetDomain))
  extensions.add(
    pgnExpand.fold[Extension](MarkdownRender.LilaLinkExtension) { MarkdownRender.PgnEmbedExtension(_) }
  )

  private val options = MutableDataSet()
    .set(Parser.EXTENSIONS, extensions)
    .set(HtmlRenderer.ESCAPE_HTML, true)
    .set(HtmlRenderer.SOFT_BREAK, "<br>")
    // always disabled
    .set(Parser.HTML_BLOCK_PARSER, false)
    .set(Parser.INDENTED_CODE_BLOCK_PARSER, false)
    .set(Parser.FENCED_CODE_BLOCK_PARSER, code)

  // configurable
  if table then options.set(TablesExtension.CLASS_NAME, "slist")
  if header then options.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, false)
  else options.set(Parser.HEADING_PARSER, false)
  if !blockQuote then options.set(Parser.BLOCK_QUOTE_PARSER, false)
  if !list then options.set(Parser.LIST_BLOCK_PARSER, false)

  private val immutableOptions = options.toImmutable

  private val parser = Parser.builder(immutableOptions).build()
  private val renderer = HtmlRenderer.builder(immutableOptions).build()

  private val logger = lila.log("markdown")

  private def mentionsToLinks(markdown: Markdown): Markdown =
    Markdown(RawHtml.atUsernameRegex.replaceAllIn(markdown.value, "[@$1](/@/$1)"))

  // https://github.com/vsch/flexmark-java/issues/496
  private val tooManyUnderscoreRegex = """(_{6,})""".r
  private def preventStackOverflow(text: Markdown) = Markdown:
    tooManyUnderscoreRegex.replaceAllIn(text.value, "_" * 3)

  def apply(key: MarkdownRender.Key)(text: Markdown): Html = Html:
    Chronometer
      .sync:
        try renderer.render(parser.parse(mentionsToLinks(preventStackOverflow(text)).value))
        catch
          case e: StackOverflowError =>
            logger.branch(key).error("StackOverflowError", e)
            text.value
      .mon(_.markdown.time)
      .logIfSlow(50, logger.branch(key))(_ => s"slow markdown size:${text.value.size}")
      .result

object MarkdownRender:

  type Key = String
  type PgnSourceId = String

  case class PgnSourceExpand(domain: NetDomain, getPgn: PgnSourceId => Option[LpvEmbed])

  private val rel = "nofollow noreferrer"

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
        "imgs.xkcd.com",
        "image.lichess1.org",
        "pic.lichess.org",
        "127.0.0.1"
      )

    private def whitelistedSrc(src: String, assetDomain: Option[AssetDomain]): Option[String] = for
      url <- lila.common.url.parse(src).toOption
      if url.scheme == "http" || url.scheme == "https"
      host <- Option(url.host).map(_.toHostString)
      if (assetDomain.toList ::: whitelist).exists(h => host == h.value || host.endsWith(s".$h"))
    yield url.toString

    def create(assetDomain: Option[AssetDomain]) = new HtmlRenderer.HtmlRendererExtension:
      override def rendererOptions(options: MutableDataHolder) = ()
      override def extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) =
        htmlRendererBuilder.nodeRendererFactory:
          new:
            override def apply(options: DataHolder) = new NodeRenderer:
              override def getNodeRenderingHandlers() =
                Set(NodeRenderingHandler(classOf[Image], render(_, _, _))).asJava

      private def render(node: Image, context: NodeRendererContext, html: HtmlWriter): Unit =
        // Based on implementation in CoreNodeRenderer.
        if context.isDoNotRenderLinks || CoreNodeRenderer.isSuppressedLinkPrefix(node.getUrl(), context) then
          context.renderChildren(node)
        else
          val resolvedLink = context.resolveLink(LinkType.IMAGE, node.getUrl().unescape(), null, null)
          val url = resolvedLink.getUrl()
          val altText = new TextCollectingVisitor().collectAndGetText(node)
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
                .attr("target", "_blank")
                .attr("rel", rel)
                .withAttr(resolvedLink)
                .tag("a")
                .text(if altText.isEmpty then url else altText)
                .tag("/a")

  private class PgnEmbedExtension(expander: PgnSourceExpand) extends HtmlRenderer.HtmlRendererExtension:
    override def rendererOptions(options: MutableDataHolder) = ()
    override def extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) =
      htmlRendererBuilder.nodeRendererFactory:
        new:
          override def apply(options: DataHolder) = new PgnEmbedNodeRenderer(expander)

  private class PgnEmbedNodeRenderer(expander: PgnSourceExpand) extends NodeRenderer:
    override def getNodeRenderingHandlers() = java.util.HashSet:
      Arrays.asList(
        NodeRenderingHandler(classOf[Link], renderLink(_, _, _)),
        NodeRenderingHandler(classOf[AutoLink], renderAutoLink(_, _, _))
      )

    final class PgnRegexes(val game: Regex, val chapter: Regex)
    private val pgnRegexes: PgnRegexes =
      val quotedDomain = java.util.regex.Pattern.quote(expander.domain.value)
      PgnRegexes(
        s"""^(?:https?://)?$quotedDomain/(?:embed/)?(?:game/)?(\\w{8})(?:(?:/(white|black))|\\w{4}|)(?:#(\\d+))?$$""".r,
        s"""^(?:https?://)?$quotedDomain/study/(?:embed/)?(?:\\w{8}/)?(\\w{8})(?:#(last|\\d+))?$$""".r
      )

    private def renderLink(node: Link, context: NodeRendererContext, html: HtmlWriter): Unit =
      renderLinkWithBase(
        node,
        context,
        html,
        context.resolveLink(LinkType.LINK, node.getUrl().unescape(), null, null)
      )

    private def renderAutoLink(node: AutoLink, context: NodeRendererContext, html: HtmlWriter): Unit =
      renderLinkNode(node, context, html)

    private def renderLinkNode(node: LinkNode, context: NodeRendererContext, html: HtmlWriter) =
      // Based on implementation in CoreNodeRenderer.
      if context.isDoNotRenderLinks || CoreNodeRenderer.isSuppressedLinkPrefix(node.getUrl(), context) then
        context.renderChildren(node)
      else
        val link = context.resolveLink(LinkType.LINK, node.getUrl().unescape(), null, null)
        def justAsLink() = renderLinkWithBase(node, context, html, link)
        link.getUrl match
          case pgnRegexes.game(id, color, ply) =>
            expander
              .getPgn(id)
              .fold(justAsLink())(renderLpvEmbed(node, context, html, link, _, Option(color), Option(ply)))
          case pgnRegexes.chapter(id, ply) =>
            expander
              .getPgn(id)
              .fold(justAsLink())(renderLpvEmbed(node, context, html, link, _, none, Option(ply)))
          case _ => justAsLink()

    private def renderLinkWithBase(
        node: LinkNode,
        context: NodeRendererContext,
        html: HtmlWriter,
        baseLink: ResolvedLink
    ) =
      val link = if node.getTitle.isNotNull then baseLink.withTitle(node.getTitle().unescape()) else baseLink
      html.attr("href", link.getUrl)
      html.attr(link.getNonNullAttributes())
      html.srcPos(node.getChars()).withAttr(link).tag("a")
      context.renderChildren(node)
      html.tag("/a")

    private def renderLpvEmbed(
        node: LinkNode,
        context: NodeRendererContext,
        html: HtmlWriter,
        link: ResolvedLink,
        embed: LpvEmbed,
        color: Option[String],
        ply: Option[String]
    ) =
      embed match
        case LpvEmbed.PublicPgn(pgn) =>
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
        case LpvEmbed.PrivateStudy =>
          html
            .attr("href", link.getUrl)
            .attr(link.getNonNullAttributes())
            .srcPos(node.getChars())
            .withAttr(link)
            .tag("a")
            .withAttr()
            .attr("class", "private-study")
            .attr("title", "Private")
            .attr("aria-label", "Private")
            .tag("i")
            .tag("/i")
          context.renderChildren(node)
          html
            .tag("/a")

  private object LilaLinkExtension extends HtmlRenderer.HtmlRendererExtension:
    override def rendererOptions(options: MutableDataHolder) = ()
    override def extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) =
      htmlRendererBuilder.attributeProviderFactory:
        new IndependentAttributeProviderFactory:
          override def apply(context: LinkResolverContext): AttributeProvider = lilaLinkAttributeProvider

  private val lilaLinkAttributeProvider = new AttributeProvider:
    override def setAttributes(node: Node, part: AttributablePart, attributes: MutableAttributes) =
      if (node.isInstanceOf[Link] || node.isInstanceOf[AutoLink]) && part == AttributablePart.LINK then
        attributes.replaceValue("target", "_blank")
        attributes.replaceValue("rel", rel)
        attributes.replaceValue("href", RawHtml.removeUrlTrackingParameters(attributes.getValue("href")))

  private val tableWrapperExtension = new HtmlRenderer.HtmlRendererExtension:
    override def rendererOptions(options: MutableDataHolder) = ()
    override def extend(builder: HtmlRenderer.Builder, rendererType: String) = builder.nodeRendererFactory:
      new NodeRendererFactory:
        override def apply(options: DataHolder) = new NodeRenderer:
          override def getNodeRenderingHandlers() = Set(
            NodeRenderingHandler(
              classOf[TableBlock],
              (_: TableBlock, context: NodeRendererContext, html: HtmlWriter) =>
                html.withAttr().attr("class", "slist-wrapper").tag("div")
                context.delegateRender();
                html.tag("/div")
            )
          ).asJava
