package lila.app
package ui

import ornicar.scalalib.Zero

import play.twirl.api.Html
import scalatags.Text.all.{ genericAttr, attr, UnitFrag }
import scalatags.Text.{ TypedTag, Frag, RawFrag, Attr, AttrValue, Cap, Aggregate, Attrs }

object Scalatags extends Scalatags {

  // twirl template minimal helpers. Allows `*.rows := 5`
  object star extends Cap with Aggregate {
    object * extends Cap with Attrs with LilaAttrs
  }
}

trait LilaAttrs {
  lazy val minlength = attr("minlength") // missing from scalatags atm
  lazy val dataTag = attr("data-tag")
  lazy val dataIcon = attr("data-icon")
  lazy val dataHint = attr("data-hint")
  lazy val dataHref = attr("data-href")
  lazy val dataCount = attr("data-count")
  lazy val dataEnableTime = attr("data-enable-time")
  lazy val datatime24h = attr("data-time_24h")
}

trait Scalatags extends LilaAttrs {

  /* Feed frags back to twirl by converting them to rendered Html */
  implicit def fragToPlayHtml(frag: Frag): Html = Html(frag.render)

  /* Use play Html inside tags without double-encoding */
  implicit def playHtmlToFrag(html: Html): Frag = RawFrag(html.body)

  /* Convert play URLs to scalatags attributes with toString */
  implicit val playCallAttr = genericAttr[play.api.mvc.Call]

  implicit val charAttr = genericAttr[Char]

  implicit val optionStringAttr = new AttrValue[Option[String]] {
    def apply(t: scalatags.text.Builder, a: Attr, v: Option[String]): Unit = {
      v foreach { s =>
        t.setAttr(a.name, scalatags.text.Builder.GenericAttrValueSource(s))
      }
    }
  }

  implicit val optionBooleanAttr = new AttrValue[Option[Boolean]] {
    def apply(t: scalatags.text.Builder, a: Attr, v: Option[Boolean]): Unit =
      if (~v) t.setAttr(a.name, scalatags.text.Builder.GenericAttrValueSource("true"))
  }

  /* for class maps such as List("foo" -> true, "active" -> isActive) */
  implicit val classesAttr = new AttrValue[List[(String, Boolean)]] {
    def apply(t: scalatags.text.Builder, a: Attr, m: List[(String, Boolean)]): Unit = {
      val cls = m collect { case (s, true) => s } mkString " "
      if (cls.nonEmpty) t.setAttr(a.name, scalatags.text.Builder.GenericAttrValueSource(cls))
    }
  }

  @inline implicit def toPimpedFrag(frag: Frag) = new PimpedFrag(frag)

  val emptyFrag: Frag = UnitFrag(())
  implicit val LilaFragZero: Zero[Frag] = Zero.instance(emptyFrag)
}

final class PimpedFrag(private val self: Frag) extends AnyVal {
  def toHtml: Html = Html(self.render)
}
