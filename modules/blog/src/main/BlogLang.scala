package lila.blog

import play.api.i18n.Lang

sealed abstract class BlogLang(val code: BlogLang.Code)

object BlogLang {
  type Code = String

  case object English  extends BlogLang("en-US")
  case object Japanese extends BlogLang("ja-JP")

  case object All extends BlogLang("*")

  val default  = English
  val allLangs = List(English, Japanese)

  def fromLangCode(langCode: Code): BlogLang =
    allLangs.find(_.code == langCode).getOrElse(default)

  def fromLang(lang: Lang): BlogLang =
    fromLangCode(lang.code)
}
