package lila.app
package templating

import java.text.NumberFormat
import java.util.Locale
import scala.collection.mutable

import lila.user.UserContext

trait NumberHelper { self: I18nHelper =>

  private val formatters = mutable.Map[String, NumberFormat]()

  private def formatter(implicit ctx: UserContext): NumberFormat =
    formatters.getOrElseUpdate(
      lang(ctx).language,
      NumberFormat getInstance new Locale(lang(ctx).language))

  def showMillis(millis: Int)(implicit ctx: UserContext) = formatter format ((millis / 100).toDouble / 10)

  implicit def richInt(number: Int) = new {
    def localize(implicit ctx: UserContext): String = formatter format number
  }

  def nth(number: Int) = if ((11 to 13).contains(number % 100))
    "th"
  else number % 10 match {
    case 1 => "st"
    case 2 => "nd"
    case 3 => "rd"
    case _ => "th"
  }
}
