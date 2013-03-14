package lila.app
package templating

import http.Context
import i18n.I18nHelper

import org.joda.time.DateTime
import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }
import java.util.Locale
import scala.collection.mutable

trait DateHelper { self: I18nHelper ⇒

  private val style = "MS"

  private val formatters = mutable.Map[String, DateTimeFormatter]()

  private def formatter(ctx: Context): DateTimeFormatter =
    formatters.getOrElseUpdate(
      lang(ctx).language,
      DateTimeFormat forStyle style withLocale new Locale(lang(ctx).language))

  def showDate(date: DateTime)(implicit ctx: Context): String =
    formatter(ctx) print date
}
