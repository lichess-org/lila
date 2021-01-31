package lila.i18n

import play.api.libs.json.{ JsObject, JsString }
import play.api.i18n.Lang

object JsDump {

  private def quantitySuffix(q: I18nQuantity): String =
    q match {
      case I18nQuantity.Zero  => ":zero"
      case I18nQuantity.One   => ":one"
      case I18nQuantity.Two   => ":two"
      case I18nQuantity.Few   => ":few"
      case I18nQuantity.Many  => ":many"
      case I18nQuantity.Other => ""
    }

  private type JsTrans = Iterable[(String, JsString)]

  def removeDbPrefix(key: MessageKey): String = {
    val index = key.indexOf(':')
    if (index > 0) key.drop(index + 1) else key
  }

  private def translatedJs(fullKey: MessageKey, t: Translation): JsTrans = {
    val k = removeDbPrefix(fullKey)
    t match {
      case literal: Simple  => List(k -> JsString(literal.message))
      case literal: Escaped => List(k -> JsString(literal.message))
      case plurals: Plurals =>
        plurals.messages.map { case (quantity, msg) =>
          s"$k${quantitySuffix(quantity)}" -> JsString(msg)
        }
    }
  }

  def keysToObject(keys: Seq[MessageKey], lang: Lang): JsObject =
    JsObject {
      keys.flatMap { k =>
        Translator.findTranslation(k, lang).fold[JsTrans](Nil) { translatedJs(k, _) }
      }
    }
}
