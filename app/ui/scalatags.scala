package lila.app
package ui

import ornicar.scalalib.Zero

import play.twirl.api.Html
import scalatags.Text.all.{ genericAttr, attr, StringFrag }
import scalatags.Text.{ Frag, RawFrag, Attr, AttrValue, Modifier, Cap, Aggregate, Attrs, Styles }
import scalatags.text.Builder

// collection of lila attrs
trait ScalatagsAttrs {
  lazy val minlength = attr("minlength") // missing from scalatags atm
  lazy val dataTag = attr("data-tag")
  lazy val dataIcon = attr("data-icon")
  lazy val dataHint = attr("data-hint")
  lazy val dataHref = attr("data-href")
  lazy val dataCount = attr("data-count")
  lazy val dataEnableTime = attr("data-enable-time")
  lazy val datatime24h = attr("data-time_24h")
  lazy val dataColor = attr("data-color")
  lazy val dataFen = attr("data-fen")
  lazy val novalidate = attr("novalidate")
}

// collection of lila snippets
trait ScalatagsSnippets extends Cap {
  this: ScalatagsExtensions with ScalatagsAttrs =>

  import scalatags.Text.all._

  val nbsp = raw("&nbsp;")
  def iconTag(icon: Char): Tag = iconTag(icon.toString)
  def iconTag(icon: String): Tag = i(dataIcon := icon)
  def iconTag(icon: String, text: Frag): Tag = i(dataIcon := icon, cls := "text")(text)

  lazy val dataBotAttr = attr("data-bot").empty

  def dataBot(title: lila.user.Title): Modifier =
    if (title == lila.user.Title.BOT) dataBotAttr
    else emptyModifier
}

// basic imports from scalatags
trait ScalatagsBundle extends Cap
  with Attrs
  with scalatags.text.Tags
  // with DataConverters
  with Aggregate

// short prefix
trait ScalatagsPrefix {
  object st extends Cap with Attrs with scalatags.text.Tags

}

// what to import in a pure scalatags template
trait ScalatagsTemplate extends Styles
  with ScalatagsBundle
  with ScalatagsAttrs
  with ScalatagsExtensions
  with ScalatagsSnippets
  with ScalatagsPrefix {

  val trans = lila.i18n.I18nKeys
}

object ScalatagsTemplate extends ScalatagsTemplate

// what to import in all twirl templates
trait ScalatagsTwirl extends ScalatagsPlay

// what to import in twirl templates containing scalatags forms
// Allows `*.rows := 5`
trait ScalatagsTwirlForm extends ScalatagsPlay with Cap with Aggregate {
  object * extends Cap with Attrs with ScalatagsAttrs
}
object ScalatagsTwirlForm extends ScalatagsTwirlForm

// interop with play
trait ScalatagsPlay {

  /* Feed frags back to twirl by converting them to rendered Html */
  implicit def fragToPlayHtml(frag: Frag): Html = Html(frag.render)

  /* Use play Html inside tags without double-encoding */
  implicit def playHtmlToFrag(html: Html): Frag = RawFrag(html.body)

  /* Convert play URLs to scalatags attributes with toString */
  implicit val playCallAttr = genericAttr[play.api.mvc.Call]

  @inline implicit def fragToHtml(frag: Frag) = new FragToHtml(frag)
}

final class FragToHtml(private val self: Frag) extends AnyVal {
  def toHtml: Html = Html(self.render)
}

// generic extensions
trait ScalatagsExtensions {

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

  val emptyFrag: Frag = new StringFrag("")
  implicit val LilaFragZero: Zero[Frag] = Zero.instance(emptyFrag)

  val emptyModifier: Modifier = new Modifier {
    def applyTo(t: Builder) = {}
  }
  // implicit val LilaModifierZero: Zero[Modifier] = Zero.instance(emptyModifier)
}
