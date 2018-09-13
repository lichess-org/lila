package lila.app
package templating

import ornicar.scalalib.Zero
import play.twirl.api.Html

import lila.base.RawHtml
import lila.user.UserContext

trait StringHelper { self: NumberHelper =>

  def netDomain: String

  val emptyHtml = Html("")
  implicit val LilaHtmlZero: Zero[Html] = Zero.instance(emptyHtml)

  val slugify = lila.common.String.slugify _

  def shorten(text: String, length: Int, sep: String = "…") = lila.common.String.shorten(text, length, sep)

  def pluralize(s: String, n: Int) = s"$n $s${if (n > 1) "s" else ""}"

  def repositionTooltipUnsafe(link: Html, position: String) = Html {
    link.body.replace("<a ", s"""<a data-pt-pos="$position" """)
  }

  def showNumber(n: Int): String = if (n > 0) s"+$n" else n.toString

  implicit def lilaRichString(str: String) = new {
    def active(other: String, one: String = "active") = if (str == other) one else ""
  }

  def when(cond: Boolean, str: String) = cond ?? str

  private val NumberFirstRegex = """(\d++)\s(.+)""".r
  private val NumberLastRegex = """\s(\d++)$""".r.unanchored
  def splitNumberUnsafe(s: String)(implicit ctx: UserContext): Html = Html {
    s match {
      case NumberFirstRegex(number, text) =>
        s"<strong>${(~parseIntOption(number)).localize}</strong><br />$text"
      case NumberLastRegex(n) if s.length > n.length + 1 =>
        s"${s.dropRight(n.length + 1)}<br /><strong>${(~parseIntOption(n)).localize}</strong>"
      case h => h.replaceIf('\n', "<br />")
    }
  }
  def splitNumber(s: Html)(implicit ctx: UserContext): Html = splitNumberUnsafe(s.body)

  def encodeFen(fen: String) = lila.common.String.base64.encode(fen).reverse

  def addQueryParameter(url: String, key: String, value: Any) =
    if (url contains "?") s"$url&$key=$value" else s"$url?$key=$value"

  def htmlList(htmls: List[Html], separator: String = ", ") = Html {
    htmls mkString separator
  }

  def camo(url: String): String

  def richText(rawText: String, nl2br: Boolean = true) = Html {
    val withLinks = RawHtml.addLinks(rawText, camo)
    if (nl2br) RawHtml.nl2br(withLinks) else withLinks
  }
}
