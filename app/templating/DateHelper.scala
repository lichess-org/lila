package lila.app
package templating

import java.util.Locale
import scala.collection.mutable

import org.joda.time.DateTime
import org.joda.time.format._
import play.api.templates.Html

import lila.api.Context

trait DateHelper { self: I18nHelper =>

  private val dateTimeStyle = "MS"
  private val dateStyle = "M-"

  private val dateTimeFormatters = mutable.Map[String, DateTimeFormatter]()
  private val dateFormatters = mutable.Map[String, DateTimeFormatter]()
  private val timeFormatter = DateTimeFormat forPattern "HH:mm"

  private val isoFormatter = ISODateTimeFormat.dateTime

  private def dateTimeFormatter(ctx: Context): DateTimeFormatter =
    dateTimeFormatters.getOrElseUpdate(
      lang(ctx).language,
      DateTimeFormat forStyle dateTimeStyle withLocale new Locale(lang(ctx).language))

  private def dateFormatter(ctx: Context): DateTimeFormatter =
    dateFormatters.getOrElseUpdate(
      lang(ctx).language,
      DateTimeFormat forStyle dateStyle withLocale new Locale(lang(ctx).language))

  def showDateTime(date: DateTime)(implicit ctx: Context): String =
    dateTimeFormatter(ctx) print date

  def showDate(date: DateTime)(implicit ctx: Context): String =
    dateFormatter(ctx) print date

  def showTimeNoCtx(date: DateTime): String = timeFormatter print date

  def timeago(date: DateTime)(implicit ctx: Context): Html = Html(
    """<time class="timeago" datetime="%s">%s</time>"""
      .format(isoFormatter print date, showDateTime(date))
  )

  def timeagoLocale(implicit ctx: Context): Option[String] =
    lang(ctx).language match {
      case "en" => none
      case "fr" => "fr-short".some
      case "pt" => "pt-br".some
      case "zh" => "zh-CN".some
      case l    => timeagoLocales(l) option l
    }

  private lazy val timeagoLocales: Set[String] = {
    import java.io.File
    val Regex = """^jquery\.timeago\.(\w{2})\.js$""".r
    (new File(Env.current.timeagoLocalesPath).listFiles map (_.getName) collect {
      case Regex(l) => l
    }).toSet: Set[String]
  }
}
