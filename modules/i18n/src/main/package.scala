package lila

import play.api.i18n.Lang

package object i18n extends PackageObject {

  type Count      = Int
  type MessageKey = String

  private[i18n] type MessageMap = java.util.Map[MessageKey, Translation]
  private[i18n] type Messages   = Map[Lang, MessageMap]

  private[i18n] def logger = lila.log("i18n")

  private[i18n] val lichessCodes: Map[String, Lang] = Map(
    "fp" -> Lang("frp", "IT"),
    "jb" -> Lang("jbo", "EN"),
    "kb" -> Lang("kab", "DZ"),
    "tc" -> Lang("zh", "CN")
  )

  val enLang      = Lang("en", "GB")
  val defaultLang = enLang
}
