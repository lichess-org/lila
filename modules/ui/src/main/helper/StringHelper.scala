package lila.ui

import lila.ui.ScalatagsTemplate.*

trait StringHelper:
  self: I18nHelper & NumberHelper =>

  def pluralize(s: String, n: Int) = s"$n $s${if n != 1 then "s" else ""}"

  def pluralizeLocalize(s: String, n: Int)(using Translate) = s"${n.localize} $s${if n != 1 then "s" else ""}"

  def showNumber(n: Int): String = if n > 0 then s"+$n" else n.toString

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
