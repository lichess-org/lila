package lila.swiss

import akka.stream.scaladsl._

// https://www.fide.com/FIDE/handbook/C04Annex2_TRF16.pdf
final class SwissTrf(
    colls: SwissColls,
    sheetApi: SwissSheetApi,
    baseUrl: lila.common.config.BaseUrl
) {

  private type Bits = List[(Int, String)]

  def apply(swiss: Swiss): Source[String, _] =
    tournamentLines(swiss) concat sheetApi
      .source(swiss)
      .map((playerLine(swiss) _).tupled)
      .map(formatLine)

  private def tournamentLines(swiss: Swiss) =
    Source(
      List(
        s"012 ${swiss.name}",
        s"022 ${baseUrl}/swiss/${swiss.id}",
        s"032 Lichess",
        s"042 ${dateFormatter print swiss.startsAt}",
        s"052 ${swiss.finishedAt ?? dateFormatter.print}",
        s"062 ${swiss.nbPlayers}",
        s"092 Individual: Swiss-System",
        s"102 ${baseUrl}/swiss",
        s"XXR ${swiss.settings.nbRounds}",
        s"XXC ${chess.Color(scala.util.Random.nextBoolean).name}1"
      )
    )

  private def playerLine(
      swiss: Swiss
  )(p: SwissPlayer, pairings: Map[SwissRound.Number, SwissPairing], sheet: SwissSheet): Bits =
    List(
      3  -> "001",
      8  -> p.number.toString,
      47 -> p.userId,
      84 -> f"${sheet.points.value}%1.1f"
    ) ::: {
      swiss.allRounds.zip(sheet.outcomes).flatMap {
        case (rn, outcome) =>
          val pairing = pairings get rn
          List(
            95 -> pairing.map(_ opponentOf p.number).??(_.toString),
            97 -> pairing.map(_ colorOf p.number).??(_.fold("w", "b")),
            99 -> {
              import SwissSheet._
              outcome match {
                case Absent  => "-"
                case Late    => "H"
                case Bye     => "F"
                case Draw    => "="
                case Win     => "1"
                case Loss    => "0"
                case Ongoing => "Z" // should not happen
              }
            }
          ).map { case (l, s) => (l + (rn.value - 1) * 10, s) }
      }
    } ::: p.absent.?? {
      List( // http://www.rrweb.org/javafo/aum/JaVaFo2_AUM.htm#_Unusual_info_extensions
        95 -> "0000",
        97 -> "",
        99 -> "-"
      ).map { case (l, s) => (l + swiss.round.value * 10, s) }
    }

  private def formatLine(bits: Bits): String =
    bits.foldLeft("") {
      case (acc, (pos, txt)) => s"""$acc${" " * (pos - txt.size - acc.size)}$txt"""
    }

  private val dateFormatter = org.joda.time.format.DateTimeFormat forStyle "M-"
}
