package lila.app
package templating

import http.Context
import i18n.I18nHelper

import java.util.Locale
import java.text.NumberFormat
import scala.collection.mutable

trait NumberHelper { self: I18nHelper ⇒

  private val formatters = mutable.Map[String, NumberFormat]()

  private def formatter(ctx: Context): NumberFormat =
    formatters.getOrElseUpdate(
      lang(ctx).language,
      NumberFormat getInstance new Locale(lang(ctx).language))

  implicit def richInt(number: Int) = new {
    def localize(implicit ctx: Context): String =
      formatter(ctx) format number
  }
}
