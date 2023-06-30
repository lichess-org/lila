package lila.app
package ui

import ornicar.scalalib.Zero

import scalatags.Text.all._
import scalatags.text.Builder
import scalatags.Text.{ Aggregate, Cap }

import lila.api.Context

// collection of lila attrs
trait ScalatagsAttrs {
  val dataAssetUrl     = attr("data-asset-url")
  val dataAssetVersion = attr("data-asset-version")
  val dataDev          = attr("data-dev")
  val dataTheme        = attr("data-theme")
  val dataPieceSet     = attr("data-piece-set")
  val dataChuPieceSet  = attr("data-chu-piece-set")
  val dataKyoPieceSet  = attr("data-kyo-piece-set")
  val dataTag          = attr("data-tag")
  val dataIcon         = attr("data-icon")
  val dataHref         = attr("data-href")
  val dataCount        = attr("data-count")
  val dataEnableTime   = attr("data-enable-time")
  val datatime24h      = attr("data-time_24h")
  val dataColor        = attr("data-color")
  val dataSfen         = attr("data-sfen")
  val dataVariant      = attr("data-variant")
  val dataRel          = attr("data-rel")
  val novalidate       = attr("novalidate").empty
  val datetimeAttr     = attr("datetime")
  val dataBotAttr      = attr("data-bot").empty
  val deferAttr        = attr("defer").empty
  object frame {
    val scrolling       = attr("scrolling")
    val allowfullscreen = attr("allowfullscreen").empty
  }
}

// collection of lila snippets
trait ScalatagsSnippets extends Cap {
  this: ScalatagsExtensions with ScalatagsAttrs =>

  import scalatags.Text.all._

  val nbsp                                   = raw("&nbsp;")
  val amp                                    = raw("&amp;")
  def iconTag(icon: Char): Tag               = iconTag(icon.toString)
  def iconTag(icon: String): Tag             = i(dataIcon := icon)
  def iconTag(icon: Char, text: Frag): Tag   = iconTag(icon.toString, text)
  def iconTag(icon: String, text: Frag): Tag = i(dataIcon := icon, cls := "text")(text)
  val styleTag                               = tag("style")(tpe := "text/css")
  val ratingTag                              = tag("rating")
  val countTag                               = tag("count")
  val goodTag                                = tag("good")
  val badTag                                 = tag("bad")
  val timeTag                                = tag("time")

  def dataBot(title: lila.user.Title): Modifier =
    if (title == lila.user.Title.BOT) dataBotAttr
    else emptyModifier

  def pagerNext(pager: lila.common.paginator.Paginator[_], url: Int => String): Option[Frag] =
    pager.nextPage.map { np =>
      div(cls := "pager none")(a(rel := "next", href := url(np))("Next"))
    }
  def pagerNextTable(pager: lila.common.paginator.Paginator[_], url: Int => String): Option[Frag] =
    pager.nextPage.map { np =>
      tr(th(cls := "pager none")(a(rel := "next", href := url(np))("Next")))
    }

  val utcLink =
    a(
      href   := "https://time.is/UTC",
      target := "_blank",
      rel    := "noopener",
      title  := "Coordinated Universal Time"
    )("UTC")
}

// basic imports from scalatags
trait ScalatagsBundle
    extends Cap
    with Attrs
    with scalatags.text.Tags
    // with DataConverters
    with Aggregate

// short prefix
trait ScalatagsPrefix {
  object st extends Cap with Attrs with scalatags.text.Tags {
    val group     = tag("group")
    val headTitle = tag("title")
    val nav       = tag("nav")
    val section   = tag("section")
    val article   = tag("article")
    val aside     = tag("aside")
    val rating    = tag("rating")

    val frameborder = attr("frameborder")
  }
}

// what to import in a pure scalatags template
trait ScalatagsTemplate
    extends Styles
    with ScalatagsBundle
    with ScalatagsAttrs
    with ScalatagsExtensions
    with ScalatagsSnippets
    with ScalatagsPrefix {

  val trans = lila.i18n.I18nKeys
  def main  = scalatags.Text.tags2.main

  /* Convert play URLs to scalatags attributes with toString */
  implicit val playCallAttr = genericAttr[play.api.mvc.Call]
}

object ScalatagsTemplate extends ScalatagsTemplate

// generic extensions
trait ScalatagsExtensions {

  implicit def stringValueFrag(sv: StringValue): Frag = new StringFrag(sv.value)

  implicit val stringValueAttr = new AttrValue[StringValue] {
    def apply(t: scalatags.text.Builder, a: Attr, v: StringValue): Unit =
      t.setAttr(a.name, scalatags.text.Builder.GenericAttrValueSource(v.value))
  }

  implicit val charAttr = genericAttr[Char]

  implicit val optionStringAttr = new AttrValue[Option[String]] {
    def apply(t: scalatags.text.Builder, a: Attr, v: Option[String]): Unit = {
      v foreach { s =>
        t.setAttr(a.name, scalatags.text.Builder.GenericAttrValueSource(s))
      }
    }
  }

  /* for class maps such as List("foo" -> true, "active" -> isActive) */
  implicit val classesAttr = new AttrValue[List[(String, Boolean)]] {
    def apply(t: scalatags.text.Builder, a: Attr, m: List[(String, Boolean)]): Unit = {
      val cls = m collect { case (s, true) => s } mkString " "
      if (cls.nonEmpty) t.setAttr(a.name, scalatags.text.Builder.GenericAttrValueSource(cls))
    }
  }

  val emptyFrag: Frag                   = new RawFrag("")
  implicit val LilaFragZero: Zero[Frag] = Zero.instance(emptyFrag)

  val emptyModifier: Modifier = new Modifier {
    def applyTo(t: Builder) = {}
  }

  val targetBlank: Modifier = (t: Builder) => {
    // Prevent tab nabbing when opening untrusted links. Apply also to trusted
    // links, because there can be a small peformance advantage and lila does
    // not use window.opener anywhere. Will not be overwritten by additional
    // rels.
    t.setAttr("rel", Builder.GenericAttrValueSource("noopener"))
    t.setAttr("target", Builder.GenericAttrValueSource("_blank"))
  }

  def ariaTitle(v: String) =
    new Modifier {
      def applyTo(t: Builder) = {
        val value = Builder.GenericAttrValueSource(v)
        t.setAttr("title", value)
        t.setAttr("aria-label", value)
      }
    }

  def titleOrText(blind: Boolean, v: String): Modifier =
    new Modifier {
      def applyTo(t: Builder) = {
        if (blind) t.addChild(v)
        else t.setAttr("title", Builder.GenericAttrValueSource(v))
      }
    }

  def titleOrText(v: String)(implicit ctx: Context): Modifier = titleOrText(ctx.blind, v)
}

object ScalatagsExtensions extends ScalatagsExtensions
