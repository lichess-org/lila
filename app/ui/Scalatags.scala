package lila.app
package ui

import play.twirl.api.Html
import scalatags.Text.{ TypedTag, Frag, RawFrag, Attr, AttrValue }
import scalatags.Text.all.{ genericAttr, attr }

object Scalatags {

  /* Feed tags back to twirl by converting them to rendered Html */
  implicit def toPlayHtml(tag: TypedTag[String]): Html = Html {
    tag.render
  }

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
}
