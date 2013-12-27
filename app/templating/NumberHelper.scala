package lila.app
package templating

import java.text.NumberFormat
import java.util.Locale
import scala.collection.mutable

import lila.user.UserContext

trait NumberHelper { self: I18nHelper ⇒

  private val formatters = mutable.Map[String, NumberFormat]()

  private def formatter(ctx: UserContext): NumberFormat =
    formatters.getOrElseUpdate(
      lang(ctx).language,
      NumberFormat getInstance new Locale(lang(ctx).language))

  implicit def richInt(number: Int) = new {
    def localize(implicit ctx: UserContext): String =
      formatter(ctx) format number
  }
}
