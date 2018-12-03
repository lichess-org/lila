package lila.app
package ui

import ornicar.scalalib.Zero

import play.twirl.api.Html
import scalatags.Text.all.{ genericAttr, attr, UnitFrag }
import scalatags.Text.{ TypedTag, Frag, RawFrag, Attr, AttrValue }

object Scalatags extends Scalatags

trait Scalatags {

  /* Feed frags back to twirl by converting them to rendered Html */
  implicit def fragToPlayHtml(frag: Frag): Html = Html(frag.render)

  /* Use play Html inside tags without double-encoding */
  implicit def playHtmlToFrag(html: Html): Frag = RawFrag(html.body)

  /* Convert play URLs to scalatags attributes with toString */
  implicit val playCallAttr = genericAttr[play.api.mvc.Call]

  lazy val dataIcon = attr("data-icon")
  lazy val dataHint = attr("data-hint")

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

  @inline implicit def toPimpedFrag(frag: Frag) = new PimpedFrag(frag)

  val emptyFrag = UnitFrag(())
  implicit val LilaFragZero: Zero[Frag] = Zero.instance(emptyFrag)
}

final class PimpedFrag(private val self: Frag) extends AnyVal {
  def toHtml: Html = Html(self.render)
}
