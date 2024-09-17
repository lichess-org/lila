package lila.tournament

import play.api.i18n.Lang

import lila.i18n.I18nKeys

sealed trait Format {
  lazy val key: String = toString.toLowerCase

  def trans(implicit lang: Lang) = Format.trans(this)
}

object Format {

  case object Arena     extends Format
  case object Robin     extends Format
  case object Organized extends Format

  val all = List(Arena, Robin, Organized)

  def byKey(k: String): Option[Format] = all.find(_.key == k)

  def trans(format: Format)(implicit lang: Lang): String =
    format match {
      case Arena     => I18nKeys.arena.arena.txt()
      case Robin     => I18nKeys.tourArrangements.roundRobin.txt()
      case Organized => I18nKeys.tourArrangements.organized.txt()
    }

}
