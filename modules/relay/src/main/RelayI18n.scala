package lila.relay

import lila.core.i18n.Translate
import lila.core.i18n.I18nKey.*

private object RelayI18n:

  // If changing, also update file://./../../../../ui/bits/src/bits.broadcastForm.i18nCheck.ts
  private val roundRegex = """(?i)^round (\d+)$""".r
  private val gameRegex = """(?i)^game (\d+)$""".r
  private val blitzRegex = """(?i)^blitz$""".r
  private val rapidRegex = """(?i)^rapid$""".r
  private val classicalRegex = """(?i)^classical$""".r
  private val openRegex = """(?i)^open$""".r
  private val womenRegex = """(?i)^women$""".r
  private val menRegex = """(?i)^men$""".r
  private val girlsRegex = """(?i)^girls$""".r
  private val boysRegex = """(?i)^boys$""".r
  private val openUnderXAgeRegex = """(?i)^open\s+u(\d{1,2})$""".r
  private val openUnderXAgeShortRegex = """(?i)^o(\d{1,2})$""".r
  private val girlsUnderXAgeRegex = """(?i)^girls\s+u(\d{1,2})$""".r
  private val girlsUnderXAgeShortRegex = """(?i)^g(\d{1,2})$""".r
  private val boysUnderXAgeRegex = """(?i)^boys\s+u(\d{1,2})$""".r
  private val boysUnderXAgeShortRegex = """(?i)^b(\d{1,2})$""".r
  private val quarterfinalsRegex = """(?i)^quarter[-\s]?final[s]?$""".r
  private val semifinalsRegex = """(?i)^semi[-\s]?final[s]?$""".r
  private val finalsRegex = """(?i)^final[s]?$""".r
  private val tiebreaksRegex = """(?i)^tie[-\s]?break(?:er)?[s]?$""".r
  private val knockoutsRegex = """(?i)^knock[-\s]?out[s]?$""".r
  private val underXAgeRegex = """(?i)^u(\d{1,2})$""".r
  private val underXEloRegex = """(?i)^u(\d{4})$""".r
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
        case menRegex() => broadcast.menTournament.txt()
        case girlsRegex() => broadcast.girlsTournament.txt()
        case boysRegex() => broadcast.boysTournament.txt()
        case openUnderXAgeRegex(age) => broadcast.openUnderXAgeTournament.txt(age)
        case openUnderXAgeShortRegex(age) => broadcast.openUnderXAgeTournament.txt(age)
        case girlsUnderXAgeRegex(age) => broadcast.girlsUnderXAgeTournament.txt(age)
        case girlsUnderXAgeShortRegex(age) => broadcast.girlsUnderXAgeTournament.txt(age)
        case boysUnderXAgeRegex(age) => broadcast.boysUnderXAgeTournament.txt(age)
        case boysUnderXAgeShortRegex(age) => broadcast.boysUnderXAgeTournament.txt(age)
        case quarterfinalsRegex() => broadcast.quarterfinals.txt()
        case semifinalsRegex() => broadcast.semifinals.txt()
        case finalsRegex() => broadcast.finals.txt()
        case tiebreaksRegex() => broadcast.tiebreaks.txt()
        case knockoutsRegex() => broadcast.knockouts.txt()
        case underXAgeRegex(age) => broadcast.underXAgeTournament.txt(age)
        case underXEloRegex(elo) => broadcast.underXEloTournament.txt(elo)
        case token => token
      .mkString(" | ")
