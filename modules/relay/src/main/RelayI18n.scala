package lila.relay

import lila.core.i18n.Translate
import lila.core.i18n.I18nKey.*

private object RelayI18n:

  private val roundRegex = """(?i)^round (\d+)$""".r
  private val gameRegex = """(?i)^game (\d+)$""".r
  private val blitzRegex = """(?i)^blitz$""".r
  private val rapidRegex = """(?i)^rapid$""".r
  private val classicalRegex = """(?i)^classical$""".r
  private val openRegex = """(?i)^open$""".r
  private val womenRegex = """(?i)^women$""".r
  private val girlsRegex = """(?i)^girls$""".r
  private val openUnderXAgeRegex = """(?i)^open\s+u(\d+)$""".r
  private val girlsUnderXAgeRegex = """(?i)^girls\s+u(\d+)$""".r
  private val quarterfinalsRegex = """(?i)^quarter[- ]?finals$""".r
  private val semifinalsRegex = """(?i)^semi[- ]?finals$""".r
  private val finalsRegex = """(?i)^finals$""".r
  private val tiebreaksRegex = """(?i)^tiebreaks$""".r
  private val knockoutsRegex = """(?i)^knock[-\s]?out[s]?$""".r
  private val sep = """\s+\|\s+""".r

  def apply(name: RelayTour.Name | RelayRound.Name)(using Translate): String =
    sep
      .split(name.toString)
      .map:
        case roundRegex(number) => broadcast.roundX.txt(number)
        case gameRegex(number) => broadcast.gameX.txt(number)
        case blitzRegex() => site.blitz.txt()
        case rapidRegex() => site.rapid.txt()
        case classicalRegex() => site.classical.txt()
        case openRegex() => broadcast.openTournament.txt()
        case womenRegex() => broadcast.womenTournament.txt()
        case girlsRegex() => broadcast.girlsTournament.txt()
        case openUnderXAgeRegex(age) => broadcast.openUnderXAgeTournament.txt(age)
        case girlsUnderXAgeRegex(age) => broadcast.girlsUnderXAgeTournament.txt(age)
        case quarterfinalsRegex() => broadcast.quarterfinals.txt()
        case semifinalsRegex() => broadcast.semifinals.txt()
        case finalsRegex() => broadcast.finals.txt()
        case tiebreaksRegex() => broadcast.tiebreaks.txt()
        case knockoutsRegex() => broadcast.knockouts.txt()
        case _ => name
      .mkString(" | ")
