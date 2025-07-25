package lila.ui

import alleycats.Zero
import cats.Monoid
import chess.PlayerTitle
import io.mola.galimatias.URL
import scalalib.Render
import scalatags.Text.all.*
import scalatags.Text.{ Aggregate, Cap, GenericAttr }
import scalatags.text.Builder
import lila.core.i18n.{ I18nKey, Translate }

// collection of lila attrs
trait ScalatagsAttrs:
  val dataTag = attr("data-tag")
  val dataIcon = attr("data-icon")
  val dataHref = attr("data-href")
  val dataCount = attr("data-count")
  val dataColor = attr("data-color")
  val dataFen = attr("data-fen")
  val dataUci = attr("data-uci")
  val dataRel = attr("data-rel")
  val dataTitle = attr("data-title")
  val novalidate = attr("novalidate").empty
  val dataBotAttr = attr("data-bot").empty
  val dataUser = attr("data-user")
  val dataUsername = attr("data-username")
  val deferAttr = attr("defer").empty
  val downloadAttr = attr("download").empty
  val viewBoxAttr = attr("viewBox")
  val enterkeyhint = attr("enterkeyhint")
  def attrData(name: String) = attr(s"data-$name")
  def aria(key: String) = attr(s"aria-$key")
  // https://accessibleweb.com/question-answer/when-should-i-use-a-null-or-empty-alt-tag/
  val emptyAlt = alt := ""

  object frame:
    val scrolling = attr("scrolling")
    val allowfullscreen = attr("allowfullscreen").empty
    val credentialless = attr("credentialless").empty

  val thSortNumber = th(attr("data-sort-method") := "number")
  val dataSort = attrData("sort")
  val dataSortDefault = attrData("sort-default").empty

// collection of lila snippets
trait ScalatagsSnippets:
  this: ScalatagsExtensions & ScalatagsAttrs =>

  import scalatags.Text.all.*

  val nbsp: Frag = raw("&nbsp;")
  val amp: Frag = raw("&amp;")
  def iconTag(icon: Icon): Tag = i(dataIcon := icon)
  def iconTag(icon: Icon, text: Frag): Tag = i(dataIcon := icon, cls := "text")(text)
  val styleTag = tag("style")
  val ratingTag = tag("rating")
  val countTag = tag("count")
  val goodTag = tag("good")
  val badTag = tag("bad")
  val timeTag = tag("time")
  val dialog = tag("dialog")
  val svgTag = tag("svg")
  val svgGroupTag = tag("g")
  val svgTextTag = tag("text")
  val details = tag("details")
  val summary = tag("summary")
  val abbr = tag("abbr")
  val boxTop = div(cls := "box__top")
  val decorativeImg = img(emptyAlt)

  def rawHtml(html: Html) = raw(html.value)

  def userTitleTag(t: PlayerTitle) = span(
    cls := "utitle",
    (t == PlayerTitle.BOT).option(dataBotAttr),
    title := PlayerTitle.titleName(t)
  )(t)

  val utcLink = a(
    href := "https://time.is/UTC",
    targetBlank,
    title := "Coordinated Universal Time"
  )("UTC")

// basic imports from scalatags
trait ScalatagsBundle extends Attrs with scalatags.text.Tags

// short prefix
trait ScalatagsPrefix:
  object st extends Cap with Attrs with scalatags.text.Tags:
    val group = tag("group")
    val headTitle = tag("title")
    val nav = tag("nav")
    val section = tag("section")
    val article = tag("article")
    val aside = tag("aside")
    val rating = tag("rating")
    val frameborder = attr("frameborder")

// what to import in a scalatags template
trait ScalatagsTemplate
    extends Cap
    with Aggregate
    with ScalatagsBundle
    with ScalatagsAttrs
    with ScalatagsExtensions
    with ScalatagsSnippets
    with ScalatagsPrefix:

  export scalatags.Text.tags2.main
  export scalatags.Text.styles.{ width as cssWidth, height as cssHeight }
  export play.api.mvc.Call

  /* Convert play URLs to scalatags attributes with toString */
  given GenericAttr[Call] = GenericAttr[Call]
  given GenericAttr[URL] = GenericAttr[URL]

object ScalatagsTemplate extends ScalatagsTemplate

// generic extensions
trait ScalatagsExtensions:

  export Context.ctxMe
  export lila.core.perm.Granter

  given Render[Icon] = _.value
  given Render[URL] = _.toString

  given [A](using Render[A]): Conversion[A, Frag] = a => StringFrag(a.render)

  given opaqueIntFrag[A](using r: IntRuntime[A]): Conversion[A, Frag] = a => intFrag(r(a))

  given [A](using Render[A]): AttrValue[A] with
    def apply(t: Builder, a: Attr, v: A): Unit = stringAttr(t, a, v.render)

  given opaqueIntAttr[A](using bts: SameRuntime[A, Int]): AttrValue[A] with
    def apply(t: Builder, a: Attr, v: A): Unit = intAttr(t, a, bts(v))

  given GenericAttr[Char] = GenericAttr[Char]
  given GenericAttr[BigDecimal] = GenericAttr[BigDecimal]

  given [A](using av: AttrValue[A]): AttrValue[Option[A]] with
    def apply(t: Builder, a: Attr, v: Option[A]): Unit = v.foreach { av.apply(t, a, _) }

  /* for class maps such as List("foo" -> true, "active" -> isActive) */
  given AttrValue[List[(String, Boolean)]] with
    def apply(t: Builder, a: Attr, m: List[(String, Boolean)]): Unit =
      val cls = m.collect { case (s, true) => s }.mkString(" ")
      if cls.nonEmpty then t.setAttr(a.name, Builder.GenericAttrValueSource(cls))

  val emptyFrag: Frag = RawFrag("")
  given Zero[Frag] = Zero(emptyFrag)

  given Monoid[Frag] with
    def empty: Frag = emptyFrag
    def combine(x: Frag, y: Frag): Frag = frag(x, y)

  val targetBlank: Modifier = (t: Builder) => t.setAttr("target", Builder.GenericAttrValueSource("_blank"))

  val noFollow = rel := "nofollow"
  val relMe = rel := "me"

  def ariaTitle(v: String): Modifier = (t: Builder) =>
    val value = Builder.GenericAttrValueSource(v)
    t.setAttr("title", value)
    t.setAttr("aria-label", value)

  def textAndTitle(i18n: I18nKey)(using Translate): Modifier = (t: Builder) =>
    t.setAttr("title", Builder.GenericAttrValueSource(i18n.txt()))
    t.addChild(i18n())

object ScalatagsExtensions extends ScalatagsExtensions
