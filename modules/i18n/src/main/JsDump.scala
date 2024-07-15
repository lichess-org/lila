package lila.i18n
import play.api.libs.json.{ JsObject, JsString }

import lila.core.i18n.{ I18nKey, Translate }

object JsDump extends lila.core.i18n.JsDump:

  private val quantitySuffix: I18nQuantity => String =
    case I18nQuantity.Zero  => ":zero"
    case I18nQuantity.One   => ":one"
    case I18nQuantity.Two   => ":two"
    case I18nQuantity.Few   => ":few"
    case I18nQuantity.Many  => ":many"
    case I18nQuantity.Other => ""

  private type JsTrans = Iterable[(String, JsString)]

  def removeDbPrefix(key: I18nKey): String =
    val index = key.value.indexOf(':')
    if index > 0 then key.value.drop(index + 1) else key.value

  private def translatedJs(fullKey: I18nKey, t: Translation): JsTrans =
    val k = removeDbPrefix(fullKey)
    t match
      case literal: Simple  => List(k -> JsString(literal.message))
      case literal: Escaped => List(k -> JsString(literal.message))
      case plurals: Plurals =>
        plurals.messages.toList match
          case List((_, msg)) => Map(k -> JsString(msg))
          case list =>
            list.map: (quantity, msg) =>
              s"$k${quantitySuffix(quantity)}" -> JsString(msg)

  def keysToObject(keys: Seq[I18nKey])(using t: Translate): JsObject =
    JsObject:
      keys.flatMap: k =>
        Registry.translation(t.lang, k).fold[JsTrans](Nil) { translatedJs(k, _) }
