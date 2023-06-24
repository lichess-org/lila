package lila.i18n

import play.api.i18n.Lang

export lila.Lila.{ *, given }

private type Count      = Long
private type MessageKey = String
private type MessageMap = java.util.Map[MessageKey, Translation]
private type Messages   = Map[Lang, MessageMap]

private val logger = lila.log("i18n")

private val lichessCodes: Map[String, Lang] = Map(
  "fp" -> Lang("frp", "IT"),
  "jb" -> Lang("jbo", "EN"),
  "kb" -> Lang("kab", "DZ"),
  "tc" -> Lang("zh", "CN")
)

val enLang      = Lang("en", "GB")
val defaultLang = enLang

// ffs
def fixJavaLanguageCode(lang: Lang) =
  val code = lang.language
  if code == "in" then "id"
  else code
