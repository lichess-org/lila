package lila.prismic

import scala.util._

import play.api.libs.functional.syntax._
import play.api.libs.json._

import org.joda.time._

trait HtmlSerializer {
  def apply(elt: Fragment.StructuredText.Element, content: String): Option[String]
}

object HtmlSerializer {
  import Fragment.StructuredText._

  def apply(f: PartialFunction[(Element, String), String]) = new HtmlSerializer {
    override def apply(elt: Element, content: String) =
      if (f.isDefinedAt((elt, content))) {
        Some(f.apply((elt, content)))
      } else {
        None
      }
  }

  def empty = new HtmlSerializer {
    override def apply(elts: Element, content: String): Option[String] = None
  }

}

sealed trait Fragment

object Fragment {

  sealed trait Link extends Fragment {
    def getUrl(linkResolver: DocumentLinkResolver): String
  }

  case class WebLink(url: String, contentType: Option[String] = None) extends Link {
    override def getUrl(linkResolver: DocumentLinkResolver) = url
    def asHtml: String                                      = s"""<a href="$url">$url</a>"""
  }

  object WebLink {

    implicit val reader: Reads[WebLink] = {
      (__ \ "url").read[String].map { case url =>
        WebLink(url)
      }
    }

  }

  case class MediaLink(url: String, kind: String, size: Long, filename: String) extends Link {
    override def getUrl(linkResolver: DocumentLinkResolver) = url
    def asHtml: String                                      = s"""<a href="$url">$filename</a>"""
  }

  object MediaLink {

    implicit val reader: Reads[MediaLink] = {
      (
        (__ \ "file" \ "url").read[String] and
          (__ \ "file" \ "kind").read[String] and
          (__ \ "file" \ "size").read[String].map(_.toLong) and
          (__ \ "file" \ "name").read[String]
      )(MediaLink.apply _)
    }
  }

  case class DocumentLink(
      id: String,
      uid: Option[String],
      typ: String,
      tags: Seq[String],
      slug: String,
      isBroken: Boolean,
  ) extends Link {
    override def getUrl(linkResolver: DocumentLinkResolver) = linkResolver(this)
    def asHtml(linkResolver: DocumentLinkResolver): String =
      s"""<a href="${linkResolver(this)}">$slug</a>"""
  }

  object DocumentLink {

    implicit val reader: Reads[DocumentLink] = {
      (
        (__ \ "document").read(
          (__ \ "id").read[String] and
            (__ \ "uid").readNullable[String] and
            (__ \ "type").read[String] and
            (__ \ "tags").readNullable[Seq[String]].map(_.getOrElse(Nil)) and
            (__ \ "slug").read[String] tupled,
        ) and
          (__ \ "isBroken").readNullable[Boolean].map(_.getOrElse(false))
      ).tupled.map(link =>
        DocumentLink(link._1._1, link._1._2, link._1._3, link._1._4, link._1._5, link._2),
      )
    }

  }

  object Link {
    implicit val reader: Reads[Link] = Reads[Link] { jsvalue =>
      (jsvalue \ "type").validate[String] flatMap {
        case "Link.web"      => Fragment.WebLink.reader.reads(jsvalue)
        case "Link.document" => Fragment.DocumentLink.reader.reads(jsvalue)
        case "Link.file"     => Fragment.MediaLink.reader.reads(jsvalue)
      }
    }
  }

  case class Text(value: String) extends Fragment {
    def asHtml: String = s"""<span class="text">$value</span>"""
  }

  object Text {
    implicit val reader: Reads[Text] = {
      Reads(v =>
        v.asOpt[String].map(d => JsSuccess(Text(d))).getOrElse(JsError(s"Invalid text value $v")),
      )
    }
  }

  case class Date(value: LocalDate) extends Fragment {
    def asText(pattern: String) = value.toString(pattern)
    def asHtml: String          = s"""<time>$value</time>"""
  }

  object Date {
    implicit val reader: Reads[Date] = {
      Reads(v =>
        v.asOpt[String]
          .flatMap(d =>
            Try(
              JsSuccess(Date(LocalDate.parse(d, format.DateTimeFormat.forPattern("yyyy-MM-dd")))),
            ).toOption,
          )
          .getOrElse(JsError(s"Invalid date value $v")),
      )
    }
  }

  case class Timestamp(value: DateTime) extends Fragment {
    def asText(pattern: String) = value.toString(pattern)
    def asHtml: String          = s"""<time>$value</time>"""
  }

  object Timestamp {
    implicit val reader: Reads[Timestamp] = {
      Reads(v =>
        v.asOpt[String]
          .flatMap { d =>
            val isoFormat = org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
            Try(
              JsSuccess(Timestamp(DateTime.parse(d, isoFormat).withZone(DateTimeZone.UTC))),
            ).toOption
          }
          .getOrElse(JsError(s"Invalid timestamp value $v")),
      )
    }
  }

  case class Number(value: Double) extends Fragment {
    def asInt                   = value.toInt
    def asText(pattern: String) = new java.text.DecimalFormat(pattern).format(value)
    def asHtml: String          = s"""<span class="number">$value</span>"""
  }

  object Number {
    implicit val reader: Reads[Number] = {
      Reads(v =>
        v.asOpt[Double]
          .map(d => JsSuccess(Number(d)))
          .getOrElse(JsError(s"Invalid number value $v")),
      )
    }
  }

  case class Color(hex: String) extends Fragment {
    def asRGB          = Color.asRGB(hex)
    def asHtml: String = s"""<span class="color">$hex</span>"""
  }

  object Color {

    private val HexColor = """#([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})""".r

    def isValidColorValue(hex: String): Boolean = hex match {
      case HexColor(_, _, _) => true
      case _                 => false
    }

    def asRGB(hex: String): (Int, Int, Int) = hex match {
      case HexColor(r, g, b) =>
        (Integer.parseInt(r, 16), Integer.parseInt(g, 16), Integer.parseInt(b, 16))
      case _ => (0, 0, 0)
    }

    implicit val reader: Reads[Color] = {
      Reads(v =>
        v.asOpt[String]
          .filter(isValidColorValue)
          .map(hex => JsSuccess(Color(hex)))
          .getOrElse(JsError(s"Invalid color value $v")),
      )
    }

  }

  case class Embed(
      typ: String,
      provider: String,
      url: String,
      width: Option[Int],
      height: Option[Int],
      html: Option[String],
      oembedJson: JsValue,
  ) extends Fragment {
    def asHtml(label: Option[String] = None): String = {
      html
        .map(html =>
          s"""<div${label
              .map(" class=\"" + _ + "\"")
              .getOrElse(
                "",
              )} data-oembed="$url" data-oembed-type="${typ.toLowerCase}" data-oembed-provider="${provider.toLowerCase}">$html</div>""",
        )
        .getOrElse("")
    }
  }

  object Embed {

    implicit val reader: Reads[Embed] = {
      (__ \ "oembed").read(
        (
          (__ \ "type").read[String] and
            (__ \ "provider_name").read[String] and
            (__ \ "embed_url").read[String] and
            (__ \ "width").readNullable[Int] and
            (__ \ "height").readNullable[Int] and
            (__ \ "html").readNullable[String] and
            __.read[JsObject]
        )(Embed.apply _),
      )
    }

  }

  case class GeoPoint(latitude: Double, longitude: Double) extends Fragment {
    def asHtml: String =
      s"""<div class="geopoint"><span class="latitude">${latitude}</span><span class="longitude">${longitude}</span></div>"""
  }

  object GeoPoint {

    implicit val reader: Reads[GeoPoint] = {
      (
        (__ \ "latitude").read[Double] and
          (__ \ "longitude").read[Double]
      )(GeoPoint.apply _)
    }

  }

  case class Image(main: Image.View, views: Map[String, Image.View] = Map.empty) extends Fragment {

    def getView(key: String): Option[Image.View] = key.toLowerCase match {
      case "main" => Some(main)
      case _      => views.get(key)
    }

    def asHtml: String = main.asHtml

  }

  object Image {

    case class View(url: String, width: Int, height: Int, alt: Option[String]) {
      def ratio = width / height
      def asHtml: String =
        s"""<img alt="${alt.getOrElse("")}" src="${url}" width="${width}" height="${height}" />"""
    }

    implicit val viewReader: Reads[View] =
      (
        (__ \ "url").read[String] and
          (__ \ "dimensions").read(
            (__ \ "width").read[Int] and
              (__ \ "height").read[Int] tupled,
          ) and
          (__ \ "alt").readNullable[String]
      ).tupled.map { case (url, (width, height), alt) =>
        View(url, width, height, alt)
      }

    implicit val reader: Reads[Image] =
      (
        (__ \ "main").read[View] and
          (__ \ "views").read[Map[String, View]]
      ).tupled.map { case (main, views) =>
        Image(main, views)
      }

  }

  case class Group(docs: Seq[Group.Doc]) extends Fragment {

    def asHtml(linkResolver: DocumentLinkResolver): String =
      docs map (_ asHtml linkResolver) mkString "\n"
  }

  object Group {

    case class Doc(fragments: Map[String, Fragment]) extends WithFragments

    implicit private val fragmentRead: Reads[Fragment] = Reads { value =>
      value.asOpt[JsObject] flatMap Document.parse match {
        case Some(f) => JsSuccess(f)
        case None    => JsError(Nil)
      }
    }

    implicit val reader: Reads[Group] =
      Reads.seq(__.read[Map[String, Fragment]]).map { docs =>
        Group(docs.map(Doc))
      }
  }

  case class StructuredText(blocks: Seq[StructuredText.Block]) extends Fragment {

    def getTitle: Option[StructuredText.Block.Heading] = blocks.collectFirst {
      case h: StructuredText.Block.Heading => h
    }

    def getFirstParagraph: Option[StructuredText.Block.Paragraph] = blocks.collectFirst {
      case p: StructuredText.Block.Paragraph => p
    }

    def getAllParagraphs: Seq[StructuredText.Block.Paragraph] = blocks.collect {
      case p: StructuredText.Block.Paragraph => p
    }

    def getFirstImage: Option[StructuredText.Block.Image] = blocks.collectFirst {
      case i: StructuredText.Block.Image => i
    }

    def asHtml(
        linkResolver: DocumentLinkResolver,
        htmlSerializer: HtmlSerializer = HtmlSerializer.empty,
    ): String = {
      StructuredText.asHtml(blocks, linkResolver, htmlSerializer)
    }

  }

  object StructuredText {

    def asHtml(
        blocks: Seq[Block],
        linkResolver: DocumentLinkResolver,
        htmlSerializer: HtmlSerializer,
    ): String = {
      case class Group(htmlTag: Option[String], blocks: Seq[Block])

      val grouped: List[Group] = blocks
        .foldLeft(List.empty[Group]) {
          case (
                (group @ Group(Some("ul"), _)) :: rest,
                block @ StructuredText.Block.ListItem(text, spans, false, label),
              ) =>
            group.copy(blocks = group.blocks :+ block) +: rest
          case (
                (group @ Group(Some("ol"), _)) :: rest,
                block @ StructuredText.Block.ListItem(text, spans, true, label),
              ) =>
            group.copy(blocks = group.blocks :+ block) +: rest
          case (groups, block @ StructuredText.Block.ListItem(text, spans, false, label)) =>
            Group(Some("ul"), Seq(block)) +: groups
          case (groups, block @ StructuredText.Block.ListItem(text, spans, true, label)) =>
            Group(Some("ol"), Seq(block)) +: groups
          case (groups, block) => Group(None, Seq(block)) +: groups
        }
        .reverse

      grouped
        .flatMap {
          case Group(Some(tag), bcks) =>
            s"<$tag>" +: bcks
              .map(block => Block.asHtml(block, linkResolver, htmlSerializer)) :+ s"</$tag>"
          case Group(None, bcks) =>
            bcks.map(block => Block.asHtml(block, linkResolver, htmlSerializer))
        }
        .mkString("\n\n")
    }

    private def asHtml(
        text: String,
        spans: Seq[Span],
        linkResolver: DocumentLinkResolver,
        serializer: HtmlSerializer,
    ): String = {

      def escape(character: String): String = {
        character.replace("<", "&lt;").replace("\n", "<br>")
      }

      def serialize(element: Element, content: String): String = {
        serializer(element, content).getOrElse {
          element match {
            case b: Block       => Block.asHtml(b, linkResolver)
            case _: Span.Em     => s"<em>$content</em>"
            case _: Span.Strong => s"<strong>$content</strong>"
            case Span.Hyperlink(_, _, link: DocumentLink) =>
              s"""<a href="${linkResolver(link)}">$content</a>"""
            case Span.Hyperlink(_, _, link: MediaLink) => s"""<a href="${link.url}">$content</a>"""
            case Span.Hyperlink(_, _, link: WebLink)   => s"""<a href="${link.url}">$content</a>"""
            case Span.Label(_, _, label) => s"""<span class="$label">$content</span>"""
            case _                       => s"<span>$content</span>"
          }
        }
      }

      case class OpenSpan(span: Span, content: String)

      @scala.annotation.tailrec
      def step(
          in: Seq[(Char, Int)],
          spans: Seq[Span],
          stack: Seq[OpenSpan] = Nil,
          html: String = "",
      ): String = {
        in match {
          case ((_, pos) :: _) if stack.headOption.map(_.span.end) == Some(pos) => {
            // Need to close a tag
            val tagHtml = serialize(stack.head.span, stack.head.content)
            stack.drop(1) match {
              case Nil    => step(in, spans, Nil, html + tagHtml)
              case h :: t => step(in, spans, h.copy(content = h.content + tagHtml) :: t, html)
            }
          }
          case ((_, pos) :: _) if spans.headOption.map(_.start) == Some(pos) => {
            // Need to open a tag
            step(in, spans.drop(1), OpenSpan(spans.head, "") +: stack, html)
          }
          case (current, _) :: tail => {
            stack match {
              case Nil =>
                // Top level
                step(tail, spans, stack, html + escape(current.toString))
              case head :: t =>
                // There is an open span, insert inside
                step(
                  tail,
                  spans,
                  head.copy(content = head.content + escape(current.toString)) :: t,
                  html,
                )
            }
          }
          case Nil =>
            stack match {
              case Nil         => html
              case head :: Nil =>
                // One last tag open, close it
                html + serialize(head.span, head.content)
              case head :: second :: tail =>
                // At least 2 tags open, close the first and continue
                step(
                  Nil,
                  spans,
                  second.copy(content =
                    second.content + serialize(head.span, head.content),
                  ) :: tail,
                  html,
                )
            }
        }
      }
      step(
        text.toList.zipWithIndex,
        spans.sortWith {
          case (a, b) if a.start == b.start => (a.end - a.start) > (b.end - b.start)
          case (a, b)                       => a.start < b.start
        },
      )
    }

    sealed trait Element

    sealed trait Span extends Element {
      def start: Int
      def end: Int
    }

    object Span {

      case class Em(start: Int, end: Int)                    extends Span
      case class Strong(start: Int, end: Int)                extends Span
      case class Hyperlink(start: Int, end: Int, link: Link) extends Span
      case class Label(start: Int, end: Int, label: String)  extends Span

      implicit val reader: Reads[Span] =
        (
          (__ \ "type").read[String] and
            (__ \ "start").read[Int] and
            (__ \ "end").read[Int] and
            (__ \ "data").readNullable[JsObject].map(_.getOrElse(Json.obj()))
        ).tupled.flatMap { case (typ, start, end, data) =>
          typ match {
            case "strong" => Reads.pure(Strong(start, end))
            case "em"     => Reads.pure(Em(start, end))
            case "hyperlink" if (data \ "type").asOpt[String].exists(_ == "Link.web") =>
              (__ \ "data" \ "value").read(WebLink.reader).map(link => Hyperlink(start, end, link))
            case "hyperlink" if (data \ "type").asOpt[String].exists(_ == "Link.document") =>
              (__ \ "data" \ "value")
                .read(DocumentLink.reader)
                .map(link => Hyperlink(start, end, link))
            case "hyperlink" if (data \ "type").asOpt[String].exists(_ == "Link.file") =>
              (__ \ "data" \ "value")
                .read(MediaLink.reader)
                .map(link => Hyperlink(start, end, link))
            case "label" =>
              (__ \ "data" \ "label").read[String].map(label => Label(start, end, label))
            case t => Reads(_ => JsError(s"Unsupported span type $t"))
          }
        }
    }

    sealed trait Block extends Element {
      def label: Option[String]
    }

    object Block {

      sealed trait Text extends Block {
        def text: String
        def spans: Seq[Span]
        override def label: Option[String]
      }

      object Text {
        def unapply(t: Text): Option[(String, Seq[Span], Option[String])] = Some(
          (t.text, t.spans, t.label),
        )
      }

      def asHtml(
          block: Block,
          linkResolver: DocumentLinkResolver,
          htmlSerializer: HtmlSerializer = HtmlSerializer.empty,
      ): String = {
        val cls = block.label match {
          case Some(label) => s""" class="$label""""
          case None        => ""
        }
        val body = block match {
          case StructuredText.Block.Text(text, spans, _) =>
            StructuredText.asHtml(text, spans, linkResolver, htmlSerializer)
          case _ => ""
        }
        htmlSerializer(block, body).getOrElse {
          block match {
            case StructuredText.Block.Heading(text, spans, level, _) =>
              s"""<h$level$cls>$body</h$level>"""
            case StructuredText.Block.Paragraph(text, spans, _)    => s"""<p$cls>$body</p>"""
            case StructuredText.Block.Preformatted(text, spans, _) => s"""<pre$cls>$body</pre>"""
            case StructuredText.Block.ListItem(text, spans, _, _)  => s"""<li$cls>$body</li>"""
            case StructuredText.Block.Image(view, hyperlink, label) => {
              val linkbody = hyperlink match {
                case Some(link: DocumentLink) =>
                  s"""<a href="${linkResolver(link)}">${view.asHtml}</a>"""
                case Some(link: WebLink)   => s"""<a href="${link.url}">${view.asHtml}</a>"""
                case Some(link: MediaLink) => s"""<a href="${link.url}">${view.asHtml}</a>"""
                case _                     => view.asHtml
              }
              s"""<p class="${(label.toSeq :+ "block-img").mkString(" ")}">$linkbody</p>"""
            }
            case StructuredText.Block.Embed(obj, label) => obj.asHtml(label)
          }
        }
      }

      case class Heading(text: String, spans: Seq[Span], level: Int, label: Option[String])
          extends Text

      object Heading {
        implicit def reader(level: Int): Reads[Heading] = (
          (__ \ "text").read[String] and
            (__ \ "spans").read(
              Reads
                .seq(Span.reader.map(Option.apply).orElse(Reads.pure(None)))
                .map(_.collect { case Some(span) => span }),
            ) and
            (__ \ "label").readNullable[String] tupled
        ).map { case (content, spans, label) =>
          Heading(content, spans, level, label)
        }
      }

      case class Paragraph(text: String, spans: Seq[Span], label: Option[String]) extends Text

      object Paragraph {
        implicit val reader: Reads[Paragraph] = (
          (__ \ "text").read[String] and
            (__ \ "spans").read(
              Reads
                .seq(Span.reader.map(Option.apply).orElse(Reads.pure(None)))
                .map(_.collect { case Some(span) => span }),
            ) and
            (__ \ "label").readNullable[String] tupled
        ).map { case (content, spans, label) =>
          Paragraph(content, spans, label)
        }
      }

      case class Preformatted(text: String, spans: Seq[Span], label: Option[String]) extends Text

      object Preformatted {
        implicit val reader: Reads[Preformatted] = (
          (__ \ "text").read[String] and
            (__ \ "spans").read(
              Reads
                .seq(Span.reader.map(Option.apply).orElse(Reads.pure(None)))
                .map(_.collect { case Some(span) => span }),
            ) and
            (__ \ "label").readNullable[String] tupled
        ).map { case (content, spans, label) =>
          Preformatted(content, spans, label)
        }
      }

      case class ListItem(text: String, spans: Seq[Span], ordered: Boolean, label: Option[String])
          extends Text

      object ListItem {
        implicit def reader(ordered: Boolean): Reads[ListItem] = (
          (__ \ "text").read[String] and
            (__ \ "spans").read(
              Reads
                .seq(Span.reader.map(Option.apply).orElse(Reads.pure(None)))
                .map(_.collect { case Some(span) => span }),
            ) and
            (__ \ "label").readNullable[String] tupled
        ).map { case (content, spans, label) =>
          ListItem(content, spans, ordered, label)
        }
      }

      case class Image(view: Fragment.Image.View, linkTo: Option[Link], label: Option[String])
          extends Block {
        def url    = view.url
        def width  = view.width
        def height = view.height
      }

      object Image {
        implicit val reader: Reads[Image] = ((__ \ "label").readNullable[String] and
          (__ \ "linkTo").readNullable[Link] and
          __.read[Fragment.Image.View] tupled).map { case (label, linkTo, view) =>
          Image(view, linkTo, label)
        }
      }

      case class Embed(obj: Fragment.Embed, label: Option[String]) extends Block

      implicit val reader: Reads[Block] = (__ \ "type").read[String].flatMap[Block] {

        case "heading1"     => __.read(Heading.reader(1)).map(identity[Block])
        case "heading2"     => __.read(Heading.reader(2)).map(identity[Block])
        case "heading3"     => __.read(Heading.reader(3)).map(identity[Block])
        case "heading4"     => __.read(Heading.reader(4)).map(identity[Block])
        case "paragraph"    => __.read(Paragraph.reader).map(identity[Block])
        case "preformatted" => __.read(Preformatted.reader).map(identity[Block])
        case "list-item"    => __.read(ListItem.reader(ordered = false)).map(identity[Block])
        case "o-list-item"  => __.read(ListItem.reader(ordered = true)).map(identity[Block])
        case "image"        => __.read(Image.reader).map(identity[Block])
        case "embed" =>
          ((__ \ "label").readNullable[String] and __.read[Fragment.Embed] tupled).map {
            case (label, obj) => Embed(obj, label): Block
          }
        case t => Reads(_ => JsError(s"Unsupported block type $t"))
      }

    }

    implicit val reader: Reads[StructuredText] = __
      .read(
        Reads.seq(Block.reader.map(Option(_)).orElse(implicitly[Reads[JsValue]].map(_ => None))),
      )
      .map(_.flatten)
      .map { case blocks =>
        StructuredText(blocks)
      }

  }

}
