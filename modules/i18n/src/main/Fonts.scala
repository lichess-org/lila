package lila.i18n

import play.api.i18n.Lang

object Fonts {

  private lazy val packs: Map[String, String] = List(
    "cyrillic" -> "av be bg ce cu cv kv mk os ru sr uk",
    "cyrillic-ext" -> "ab ba kk ky mn tg",
    "devanagari" -> "hi ne mr",
    "greek" -> "el",
    "latin-ext" -> "cs cy gn hu mi ro sm tr ty pl",
    "vietnamese" -> "vi"
  ).foldLeft(Map[String, String]()) {
    case (acc, (pack, langs)) => langs.split(' ').foldLeft(acc) {
      case (acc, lang) => acc + (lang -> pack)
    }
  }

  private val defaultPack = "latin"

  def apply(lang: Lang) = packs get lang.language getOrElse defaultPack
}
