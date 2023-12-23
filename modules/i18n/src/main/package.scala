package lila.i18n

import play.api.i18n.Lang

export lila.Lila.{ *, given }

private type Count      = Long
private type MessageKey = String
private type MessageMap = java.util.Map[MessageKey, Translation]
private type Messages   = Map[Lang, MessageMap]

private val logger = lila.log("i18n")

/* play.api.i18n.Lang is composed of language and country.
 * Let's make new types for those so we don't mix them.
 */
opaque type Language = String
object Language extends OpaqueString[Language]:
  def apply(lang: Lang): Language = lang.language

opaque type Country = String
object Country extends OpaqueString[Country]:
  def apply(lang: Lang): Country = lang.country

private val lichessCodes: Map[String, Lang] = Map(
  "fp" -> Lang("frp", "IT"),
  "jb" -> Lang("jbo", "EN"),
  "kb" -> Lang("kab", "DZ"),
  "tc" -> Lang("zh", "CN")
)

val defaultLanguage: Language = "en"
val enLang                    = Lang("en", "GB")
val defaultLang               = enLang

// ffs
def fixJavaLanguage(lang: Lang): Language =
  val l = lang.language
  if l == "in" then "id"
  else l
