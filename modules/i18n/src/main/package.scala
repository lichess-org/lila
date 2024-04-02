package lila.i18n

import play.api.i18n.Lang
import lila.core.i18n.Language

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private type Count      = Long
private type MessageKey = String
private type MessageMap = java.util.Map[MessageKey, Translation]

private val logger = lila.log("i18n")

private val lichessCodes: Map[String, Lang] = Map(
  "fp" -> Lang("frp", "IT"),
  "jb" -> Lang("jbo", "EN"),
  "kb" -> Lang("kab", "DZ"),
  "tc" -> Lang("zh", "CN")
)

// ffs
def fixJavaLanguage(lang: Lang): Language =
  Language(lang).map: l =>
    if l == "in" then "id" else l
