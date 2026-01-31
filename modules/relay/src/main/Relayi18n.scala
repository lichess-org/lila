package lila.relay

object Relayi18n:
  import lila.core.i18n.{ I18nKey, Translate }
  private val roundRegex = """(?i)^Round (\d+)$""".r
  private val blitzRegex = """(?i)^blitz$""".r
  private val rapidRegex = """(?i)^rapid$""".r
  private val classicalRegex = """(?i)^classical$""".r
  private val openRegex = """(?i)^open$""".r
  private val womenRegex = """(?i)^women$""".r
  private val boysRegex = """(?i)^boys$""".r
  private val girlsRegex = """(?i)^girls$""".r
  private val boysUnderXRegex = """(?i)^boys\s+u(\d+)$""".r
  private val girlsUnderXRegex = """(?i)^girls\s+u(\d+)$""".r
  private val quarterfinalsRegex = """(?i)^quarter[- ]?finals$""".r
  private val semifinalsRegex = """(?i)^semi[- ]?finals$""".r
  private val finalsRegex = """(?i)^finals$""".r
  private val tiebreaksRegex = """(?i)^tiebreaks$""".r
  private val sep = """\s+\|\s+""".r
  def apply(name: String)(using Translate): String =
    sep
      .split(name)
      .map: part =>
        part match
          case roundRegex(number) => I18nKey.broadcast.roundX.txt(number)
          case blitzRegex() => I18nKey.site.blitz.txt()
          case rapidRegex() => I18nKey.site.rapid.txt()
          case classicalRegex() => I18nKey.site.classical.txt()
          case openRegex() => I18nKey.broadcast.openTournament.txt()
          case womenRegex() => I18nKey.broadcast.womenTournament.txt()
          case boysRegex() => I18nKey.broadcast.boysTournament.txt()
          case girlsRegex() => I18nKey.broadcast.girlsTournament.txt()
          case boysUnderXRegex(age) => I18nKey.broadcast.boysUnderXTournament.txt(age)
          case girlsUnderXRegex(age) => I18nKey.broadcast.girlsUnderXTournament.txt(age)
          case quarterfinalsRegex() => I18nKey.broadcast.quarterfinals.txt()
          case semifinalsRegex() => I18nKey.broadcast.semifinals.txt()
          case finalsRegex() => I18nKey.broadcast.finals.txt()
          case tiebreaksRegex() => I18nKey.broadcast.tiebreaks.txt()
          case _ => name
      .mkString(" | ")
