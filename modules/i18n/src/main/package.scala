package lila

import play.api.i18n.Lang

package object i18n extends PackageObject {

  type Count      = Int
  type MessageKey = String

  private[i18n] type MessageMap = java.util.Map[MessageKey, Translation]
  private[i18n] type Messages   = Map[Lang, MessageMap]

  private[i18n] def logger = lila.log("i18n")

  val enLang      = Lang("en", "GB")
  val defaultLang = enLang

  // just language if possible, script code for chinese
  // don't bother with pt-BR for now
  def languageCode(lang: Lang) = {
    val lcode = lang.language
    if (lcode == "zh") {
      if (lang.country == "TW") "zh-Hant"
      else "zh-Hans"
    } else lcode
  }
}
