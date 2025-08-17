package lila.ui

import java.text.NumberFormat

import lila.core.i18n.{ maxLangs, Translate }

object NumberHelper:
  val formatters = scalalib.ConcurrentMap[String, NumberFormat](maxLangs)
  def formatter(using t: Translate): NumberFormat =
    formatters.computeIfAbsentAlways(t.lang.language):
      NumberFormat.getInstance(t.lang.toLocale)

trait NumberHelper:

  import NumberHelper.*

  def showMillis(millis: Int)(using Translate) = formatter.format((millis / 100).toDouble / 10)

  extension (e: Int) def localize(using Translate): String = formatter.format(e)
  extension (e: Long) def localize(using Translate): String = formatter.format(e)
