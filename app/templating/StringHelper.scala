package lidraughts.app
package templating

import ornicar.scalalib.Zero
import play.twirl.api.Html

import lidraughts.user.UserContext

trait StringHelper { self: NumberHelper =>

  def netDomain: String

  val emptyHtml = Html("")
  implicit val LidraughtsHtmlZero: Zero[Html] = Zero.instance(emptyHtml)

  val slugify = lidraughts.common.String.slugify _

  def shorten(text: String, length: Int, sep: String = "…") = lidraughts.common.String.shorten(text, length, sep)

  def pluralize(s: String, n: Int) = s"$n $s${if (n > 1) "s" else ""}"

  def repositionTooltipUnsafe(link: Html, position: String) = Html {
    link.body.replace("<a ", s"""<a data-pt-pos="$position" """)
  }

  def showNumber(n: Int): String = if (n > 0) s"+$n" else n.toString

  implicit def lidraughtsRichString(str: String) = new {
    def active(other: String, one: String = "active") = if (str == other) one else ""
  }

  def when(cond: Boolean, str: String) = cond ?? str

  private val NumberFirstRegex = """^(\d+)\s(.+)$""".r
  private val NumberLastRegex = """^(.+)\s(\d+)$""".r
  def splitNumber(s: String)(implicit ctx: UserContext): Html = Html {
    s match {
      case NumberFirstRegex(number, text) =>
        s"<strong>${(~parseIntOption(number)).localize}</strong><br />$text"
      case NumberLastRegex(text, number) =>
        s"$text<br /><strong>${(~parseIntOption(number)).localize}</strong>"
      case h => h.replace("\n", "<br />")
    }
  }
  def splitNumber(s: Html)(implicit ctx: UserContext): Html = splitNumber(s.body)

  def encodeFen(fen: String) = lidraughts.common.String.base64.encode(fen).reverse

  def addQueryParameter(url: String, key: String, value: Any) =
    if (url contains "?") s"$url&$key=$value" else s"$url?$key=$value"

  def htmlList(htmls: List[Html], separator: String = ", ") = Html {
    htmls mkString separator
  }
}
