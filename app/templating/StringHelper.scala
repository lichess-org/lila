package lidraughts.app
package templating

import lidraughts.user.UserContext
import ui.ScalatagsTemplate._

trait StringHelper { self: NumberHelper =>

  def netDomain: String

  val slugify = lidraughts.common.String.slugify _

  def shorten(text: String, length: Int, sep: String = "…") = lidraughts.common.String.shorten(text, length, sep)

  def pluralize(s: String, n: Int) = s"$n $s${if (n > 1) "s" else ""}"

  def showNumber(n: Int): String = if (n > 0) s"+$n" else n.toString

  implicit def lidraughtsRichString(str: String) = new {
    def active(other: String, one: String = "active") = if (str == other) one else ""
    def activeO(other: String, one: String = "active") = if (str == other) Some(one) else None
  }

  def when(cond: Boolean, str: String) = cond ?? str

  private val NumberFirstRegex = """(\d++)\s(.+)""".r
  private val NumberLastRegex = """\s(\d++)$""".r.unanchored

  def splitNumber(s: Frag)(implicit ctx: UserContext): Frag = {
    val rendered = s.render
    rendered match {
      case NumberFirstRegex(number, html) => frag(
        strong((~parseIntOption(number)).localize),
        br,
        raw(html)
      )
      case NumberLastRegex(n) if rendered.length > n.length + 1 => frag(
        raw(rendered.dropRight(n.length + 1)),
        br,
        strong((~parseIntOption(n)).localize)
      )
      case h => raw(h.replaceIf('\n', "<br>"))
    }
  }

  def encodeFen(fen: String) = lidraughts.common.String.base64.encode(fen).reverse

  def addQueryParameter(url: String, key: String, value: Any) =
    if (url contains "?") s"$url&$key=$value" else s"$url?$key=$value"

  def fragList(frags: List[Frag], separator: String = ", "): Frag = frags match {
    case Nil => emptyFrag
    case one :: Nil => one
    case first :: rest => first :: rest.map { f => frag(separator, f) }
  }
}
