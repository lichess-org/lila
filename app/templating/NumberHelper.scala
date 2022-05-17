package lila.app
package templating

import java.text.NumberFormat
import java.util.Locale
import play.api.i18n.Lang
import scala.collection.mutable

trait NumberHelper { self: I18nHelper =>

  private val formatters = mutable.AnyRefMap.empty[String, NumberFormat]

  private def formatter(implicit lang: Lang): NumberFormat =
    formatters.getOrElseUpdate(lang.language, makeNumberFormatAndThrowIfNull)

  private def makeNumberFormatAndThrowIfNull(implicit lang: Lang): NumberFormat = {
    val inst = NumberFormat getInstance new Locale(lang.language)
    // happens sometimes during start
    assert(inst != null, "NumberFormat.getInstance not null")
    inst
  }

  def showMillis(millis: Int)(implicit lang: Lang) = formatter.format((millis / 100).toDouble / 10)

  implicit final class RichInt(number: Int) {
    def localize(implicit lang: Lang): String = formatter format number
  }

  implicit final class RichLong(number: Long) {
    def localize(implicit lang: Lang): String = formatter format number
  }
}
