package lila.app
package templating

import java.text.NumberFormat
import java.util.Locale
import scala.collection.mutable.AnyRefMap

import lila.user.UserContext

trait NumberHelper { self: I18nHelper =>

  private val formatters = AnyRefMap.empty[String, NumberFormat]

  private def formatter(implicit ctx: UserContext): NumberFormat =
    formatters.getOrElseUpdate(
      ctx.lang.language,
      NumberFormat getInstance new Locale(ctx.lang.language)
    )

  def showMillis(millis: Int)(implicit ctx: UserContext) = formatter.format((millis / 100).toDouble / 10)

  implicit def richInt(number: Int) = new {
    def localize(implicit ctx: UserContext): String = formatter format number
  }
}
