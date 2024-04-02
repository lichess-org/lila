package lila.common

import lila.core.Strings
import lila.core.Ints
import lila.core.IpAddress

import scalalib.Iso as LibIso

object Iso:

  export scalalib.Iso
  export scalalib.Iso.*

  def strings(sep: String): StringIso[Strings] =
    LibIso[String, Strings](
      str => Strings(str.split(sep).iterator.map(_.trim).toList),
      strs => strs.value.mkString(sep)
    )
  def ints(sep: String): StringIso[Ints] =
    LibIso[String, Ints](
      str => Ints(str.split(sep).iterator.map(_.trim).flatMap(_.toIntOption).toList),
      strs => strs.value.mkString(sep)
    )

  given StringIso[IpAddress] = string[IpAddress](IpAddress.unchecked, _.value)

  import play.api.i18n.Lang
  given StringIso[Lang] = string[Lang](Lang.apply, _.toString)
