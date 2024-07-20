package lila.ui

import java.text.NumberFormat
import java.util.concurrent.ConcurrentHashMap

import lila.core.i18n.Translate

object NumberHelper:
  val formatters = new ConcurrentHashMap[String, NumberFormat]
  def formatter(using t: Translate): NumberFormat =
    formatters.computeIfAbsent(
      t.lang.language,
      _ => NumberFormat.getInstance(t.lang.toLocale)
    )

trait NumberHelper:

  import NumberHelper.*

  def showMillis(millis: Int)(using Translate) = formatter.format((millis / 100).toDouble / 10)

  extension (e: Int) def localize(using Translate): String  = formatter.format(e)
  extension (e: Long) def localize(using Translate): String = formatter.format(e)
