package lila.app
package templating

import play.api.i18n.Lang
import ui.ScalatagsTemplate.*

trait StringHelper { self: I18nHelper with NumberHelper =>

  export lila.common.String.{ slugify, shorten, urlencode, addQueryParam, addQueryParams, underscoreFen }

  def pluralize(s: String, n: Int) = s"$n $s${if (n != 1) "s" else ""}"

  def pluralizeLocalize(s: String, n: Int)(using Lang) = s"${n.localize} $s${if (n != 1) "s" else ""}"

  def showNumber(n: Int): String = if (n > 0) s"+$n" else n.toString

  private val NumberFirstRegex = """(\d++)\s(.+)""".r
  private val NumberLastRegex  = """\s(\d++)$""".r.unanchored

  def splitNumber(s: Frag)(using Lang): Frag =
    val rendered = s.render
    rendered match
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

  def fragList(frags: List[Frag], separator: String = ", "): Frag =
    frags match
      case Nil        => emptyFrag
      case one :: Nil => one
      case first :: rest =>
        RawFrag(
          frag(first :: rest.map { frag(separator, _) }).render
        )

  extension (e: String)
    def active(other: String, one: String = "active")  = if e == other then one else ""
    def activeO(other: String, one: String = "active") = e == other option one
}
