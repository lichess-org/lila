package lila.ui

import play.api.i18n.Lang

import java.text.NumberFormat
import java.util.concurrent.ConcurrentHashMap
import lila.core.i18n.Translate

trait NumberHelper:
  def showMillis(millis: Int)(using Translate): String
  extension (e: Int) def localize(using Translate): String
  extension (e: Long) def localize(using Translate): String

object NumberHelper extends NumberHelper:

  private val formatters = new ConcurrentHashMap[String, NumberFormat]

  private def formatter(using t: Translate): NumberFormat =
    formatters.computeIfAbsent(
      t.lang.language,
      _ => NumberFormat.getInstance(t.lang.toLocale)
    )

  def showMillis(millis: Int)(using Translate) = formatter.format((millis / 100).toDouble / 10)

  extension (e: Int) def localize(using Translate): String  = formatter.format(e)
  extension (e: Long) def localize(using Translate): String = formatter.format(e)
