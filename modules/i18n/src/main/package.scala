package lila

import play.api.i18n.Lang

package object i18n extends PackageObject with WithPlay {

  type Messages = Map[Lang, Map[String, String]]

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
