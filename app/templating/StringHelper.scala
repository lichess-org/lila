package lila.app
package templating

import play.api.i18n.Lang

import ui.ScalatagsTemplate._

trait StringHelper { self: NumberHelper =>

  val slugify = lila.common.String.slugify _

  val urlencode = lila.common.String.urlencode _

  def shorten(text: String, length: Int, sep: String = "…") = lila.common.String.shorten(text, length, sep)

  def pluralize(s: String, n: Int) = s"$n $s${if (n > 1) "s" else ""}"

  def showNumber(n: Int): String = if (n > 0) s"+$n" else n.toString

  private val NumberFirstRegex = """(\d++)\s(.+)""".r
  private val NumberLastRegex  = """\s(\d++)$""".r.unanchored

  def splitNumber(s: Frag)(implicit lang: Lang): Frag = {
    val rendered = s.render
    rendered match {
      case NumberFirstRegex(number, html) =>
        frag(
          strong((~number.toIntOption).localize),
          br,
          raw(html)
        )
      case NumberLastRegex(n) if rendered.length > n.length + 1 =>
        frag(
          raw(rendered.dropRight(n.length + 1)),
          br,
          strong((~n.toIntOption).localize)
        )
      case h => raw(h.replaceIf('\n', "<br>"))
    }
  }

  def encodeFen(fen: String) = lila.common.String.base64.encode(fen).reverse

  def addQueryParameter(url: String, key: String, value: Any) =
    if (url contains "?") s"$url&$key=$value" else s"$url?$key=$value"

  def fragList(frags: List[Frag], separator: String = ", "): Frag =
    frags match {
      case Nil        => emptyFrag
      case one :: Nil => one
      case first :: rest =>
        RawFrag(
          frag(first :: rest.map { frag(separator, _) }).render
        )
    }

  implicit def lilaRichString(str: String): LilaRichString = new LilaRichString(str)
}

final class LilaRichString(val str: String) extends AnyVal {
  def active(other: String, one: String = "active")  = if (str == other) one else ""
  def activeO(other: String, one: String = "active") = if (str == other) Some(one) else None
}
