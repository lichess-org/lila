package lila.app
package ui

import play.twirl.api.Html
import scalatags.Text.all.{ genericAttr, attr }
import scalatags.Text.{ TypedTag, Frag, RawFrag, Attr, AttrValue }

object Scalatags {

  type Ttag = TypedTag[String]

  /* Feed frags back to twirl by converting them to rendered Html */
  implicit def toPlayHtml(frag: Frag): Html = Html(frag.render)

  /* Convert play URLs to scalatags attributes with toString */
  implicit val playCallAttr = genericAttr[play.api.mvc.Call]

  /* Use play Html inside tags without double-encoding */
  implicit def playHtmlFrag(html: Html): Frag = RawFrag(html.body)

  lazy val dataIcon = attr("data-icon")

  implicit val charAttr = genericAttr[Char]

  implicit val optionStringAttr = new AttrValue[Option[String]] {
    def apply(t: scalatags.text.Builder, a: Attr, v: Option[String]): Unit = {
      v foreach { s =>
        t.setAttr(a.name, scalatags.text.Builder.GenericAttrValueSource(s))
      }
    }
  }

  /* for class maps such as Map("foo" -> true, "active" -> isActive) */
  implicit val stringMapAttr = new AttrValue[Map[String, Boolean]] {
    def apply(t: scalatags.text.Builder, a: Attr, m: Map[String, Boolean]): Unit = {
      val cls = m collect { case (s, true) => s } mkString " "
      if (cls.nonEmpty) t.setAttr(a.name, scalatags.text.Builder.GenericAttrValueSource(cls))
    }
  }
}
