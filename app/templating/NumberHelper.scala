package lila.app
package templating

import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import play.api.i18n.Lang
import scala.collection.mutable

trait NumberHelper { self: I18nHelper =>

  private val formatters = new ConcurrentHashMap[String, NumberFormat]

  private def formatter(using lang: Lang): NumberFormat =
    formatters.computeIfAbsent(
      lang.language,
      _ => NumberFormat.getInstance(lang.toLocale)
    )

  def showMillis(millis: Int)(using lang: Lang) = formatter.format((millis / 100).toDouble / 10)

  implicit final class RichInt(number: Int):
    def localize(using lang: Lang): String = formatter format number

  implicit final class RichLong(number: Long):
    def localize(using lang: Lang): String = formatter format number
}
