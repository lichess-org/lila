package lila.app
package ui

import alleycats.Zero
import scalatags.Text.all.*
import scalatags.text.Builder
import scalatags.Text.GenericAttr
import scalatags.Text.{ Aggregate, Cap }

import lila.api.WebContext
import lila.user.Title
import lila.common.licon.Icon

// collection of lila attrs
trait ScalatagsAttrs:
  val dataTag                = attr("data-tag")
  val dataIcon               = attr("data-icon")
  val dataHref               = attr("data-href")
  val dataCount              = attr("data-count")
  val dataColor              = attr("data-color")
  val dataFen                = attr("data-fen")
  val dataUci                = attr("data-uci")
  val dataRel                = attr("data-rel")
  val dataTitle              = attr("data-title")
  val novalidate             = attr("novalidate").empty
  val datetimeAttr           = attr("datetime")
  val dataBotAttr            = attr("data-bot").empty
  val deferAttr              = attr("defer").empty
  val downloadAttr           = attr("download").empty
  val viewBoxAttr            = attr("viewBox")
  val enterkeyhint           = attr("enterkeyhint")
  def attrData(name: String) = attr(s"data-$name")
  def aria(key: String)      = attr(s"aria-$key")

  object frame:
    val scrolling       = attr("scrolling")
    val allowfullscreen = attr("allowfullscreen").empty

  val dataSortNumberTh = th(attr("data-sort-method") := "number")
  val dataSort         = attr("data-sort")
  val dataSortDefault  = attr("data-sort-default").empty

// collection of lila snippets
trait ScalatagsSnippets:
  this: ScalatagsExtensions with ScalatagsAttrs =>

  import scalatags.Text.all.*

  val nbsp: Frag                           = raw("&nbsp;")
  val amp: Frag                            = raw("&amp;")
  def iconTag(icon: Icon): Tag             = i(dataIcon := icon)
  def iconTag(icon: Icon, text: Frag): Tag = i(dataIcon := icon, cls := "text")(text)
  val styleTag                             = tag("style")
  val ratingTag                            = tag("rating")
  val countTag                             = tag("count")
  val goodTag                              = tag("good")
  val badTag                               = tag("bad")
  val timeTag                              = tag("time")
  val dialog                               = tag("dialog")
  val svgTag                               = tag("svg")
  val svgGroupTag                          = tag("g")
  val svgTextTag                           = tag("text")
  val details                              = tag("details")
  val summary                              = tag("summary")
  val abbr                                 = tag("abbr")
  val boxTop                               = div(cls := "box__top")

  def rawHtml(html: Html) = raw(html.value)

  def userTitleTag(t: UserTitle) =
    span(
      cls := "utitle",
      t == lila.user.Title.BOT option dataBotAttr,
      title := Title titleName t
    )(t)

  val utcLink =
    a(
      href := "https://time.is/UTC",
      targetBlank,
      title := "Coordinated Universal Time"
    )("UTC")

// basic imports from scalatags
trait ScalatagsBundle extends Attrs with scalatags.text.Tags

// short prefix
trait ScalatagsPrefix:
  object st extends Cap with Attrs with scalatags.text.Tags:
    val group     = tag("group")
    val headTitle = tag("title")
    val nav       = tag("nav")
    val section   = tag("section")
    val article   = tag("article")
    val aside     = tag("aside")
    val rating    = tag("rating")

    val frameborder = attr("frameborder")

// what to import in a pure scalatags template
trait ScalatagsTemplate
    extends Cap
    with Aggregate
    with ScalatagsBundle
    with ScalatagsAttrs
    with ScalatagsExtensions
    with ScalatagsSnippets
    with ScalatagsPrefix:

  val trans     = lila.i18n.I18nKeys
  def main      = scalatags.Text.tags2.main
  def cssWidth  = scalatags.Text.styles.width
  def cssHeight = scalatags.Text.styles.height

  /* Convert play URLs to scalatags attributes with toString */
  given GenericAttr[play.api.mvc.Call] = GenericAttr[play.api.mvc.Call]

object ScalatagsTemplate extends ScalatagsTemplate

// generic extensions
trait ScalatagsExtensions:

  given Conversion[StringValue, scalatags.Text.Frag] = sv => StringFrag(sv.value)

  // TODO implicit?
  implicit def opaqueStringFrag[A](a: A)(using r: StringRuntime[A]): Frag = stringFrag(r(a))
  implicit def opaqueIntFrag[A](a: A)(using r: IntRuntime[A]): Frag       = intFrag(r(a))

  given opaqueStringAttr[A](using bts: StringRuntime[A]): AttrValue[A] with
    def apply(t: Builder, a: Attr, v: A): Unit = stringAttr(t, a, bts(v))

  given opaqueIntAttr[A](using bts: SameRuntime[A, Int]): AttrValue[A] with
    def apply(t: Builder, a: Attr, v: A): Unit = intAttr(t, a, bts(v))

  given AttrValue[StringValue] with
    def apply(t: Builder, a: Attr, v: StringValue): Unit =
      t.setAttr(a.name, Builder.GenericAttrValueSource(v.value))

  given GenericAttr[Char]       = GenericAttr[Char]
  given GenericAttr[BigDecimal] = GenericAttr[BigDecimal]

  given [A](using av: AttrValue[A]): AttrValue[Option[A]] with
    def apply(t: Builder, a: Attr, v: Option[A]): Unit = v foreach { av.apply(t, a, _) }

  /* for class maps such as List("foo" -> true, "active" -> isActive) */
  given AttrValue[List[(String, Boolean)]] with
    def apply(t: Builder, a: Attr, m: List[(String, Boolean)]): Unit =
      val cls = m collect { case (s, true) => s } mkString " "
      if (cls.nonEmpty) t.setAttr(a.name, Builder.GenericAttrValueSource(cls))

  val emptyFrag: Frag = new RawFrag("")
  given Zero[Frag]    = Zero(emptyFrag)

  val targetBlank: Modifier = (t: Builder) => {
    // Prevent tab nabbing when opening untrusted links. Apply also to trusted
    // links, because there can be a small performance advantage and lila does
    // not use window.opener anywhere. Will not be overwritten by additional
    // rels.
    t.setAttr("rel", Builder.GenericAttrValueSource("noopener"))
    t.setAttr("target", Builder.GenericAttrValueSource("_blank"))
  }

  val noFollow = rel := "nofollow"

  def ariaTitle(v: String): Modifier = (t: Builder) => {
    val value = Builder.GenericAttrValueSource(v)
    t.setAttr("title", value)
    t.setAttr("aria-label", value)
  }

  def titleOrText(blind: Boolean, v: String): Modifier = (t: Builder) =>
    if (blind) t.addChild(StringFrag(v))
    else t.setAttr("title", Builder.GenericAttrValueSource(v))

  def titleOrText(v: String)(using ctx: WebContext): Modifier = titleOrText(ctx.blind, v)

object ScalatagsExtensions extends ScalatagsExtensions
