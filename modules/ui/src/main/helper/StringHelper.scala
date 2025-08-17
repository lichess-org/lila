package lila.ui

import lila.ui.ScalatagsTemplate.*

trait StringHelper:
  self: I18nHelper & NumberHelper =>

  def pluralize(s: String, n: Int) = s"$n $s${if n != 1 then "s" else ""}"

  def pluralizeLocalize(s: String, n: Int)(using Translate) = s"${n.localize} $s${if n != 1 then "s" else ""}"

  def showNumber(n: Int): String = if n > 0 then s"+$n" else n.toString

  private val NumberFirstRegex = """(\d++)\s(.+)""".r
  private val NumberLastRegex = """\s(\d++)$""".r.unanchored

  def splitNumber(s: Frag)(using Translate): Frag =
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
      case Nil => emptyFrag
      case one :: Nil => one
      case first :: rest =>
        RawFrag:
          frag(first :: rest.map { frag(separator, _) }).render

  extension (e: String)
    def active(other: String) = if e == other then "active" else ""
    def activeO(other: String) = Option.when(e == other)("active")
