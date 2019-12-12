package lila

import lila.common.Lang

package object i18n extends PackageObject {

  type Count = Int
  type MessageKey = String

  /* Implemented by mutable.AnyRefMap.
   * Of course we don't need or use the mutability;
   * it's just that AnyRefMap is the fastest scala hashmap implementation
   */
  private[i18n] type MessageMap = java.util.Map[MessageKey, Translation]
  private[i18n] type Messages = Map[play.api.i18n.Lang, MessageMap]

  private[i18n] def logger = lila.log("i18n")

  private[i18n] val lichessCodes: Map[String, Lang] = Map(
    "fp" -> Lang("frp", "IT"),
    "jb" -> Lang("jbo", "EN"),
    "kb" -> Lang("kab", "KAB"),
    "tc" -> Lang("zh", "CN")
  )

  val enLang = Lang("en", "GB")
  val defaultLang = enLang
}
