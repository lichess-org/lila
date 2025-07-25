package lila.swiss

import akka.stream.scaladsl.*
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

// https://www.fide.com/FIDE/handbook/C04Annex2_TRF16.pdf
final class SwissTrf(
    sheetApi: SwissSheetApi,
    mongo: SwissMongo,
    baseUrl: lila.core.config.BaseUrl
)(using Executor):

  private type Bits = List[(Int, String)]

  def apply(swiss: Swiss, sorted: Boolean): Source[String, ?] = Source.futureSource:
    fetchPlayerIds(swiss).map { apply(swiss, _, sorted) }

  def apply(swiss: Swiss, playerIds: PlayerIds, sorted: Boolean): Source[String, ?] =
    SwissPlayer.fields { f =>
      tournamentLines(swiss)
        .concat(forbiddenPairings(swiss, playerIds))
        .concat:
          sheetApi
            .source(swiss, sort = sorted.so($doc(f.rating -> -1)))
            .map((playerLine(swiss, playerIds)).tupled)
            .map(formatLine)
    }

  private def tournamentLines(swiss: Swiss) =
    Source(
      List(
        s"012 ${swiss.name}",
        s"022 $baseUrl/swiss/${swiss.id}",
        s"032 Lichess",
        s"042 ${dateFormatter.print(swiss.startsAt)}",
        s"052 ${swiss.finishedAt.so(dateFormatter.print)}",
        s"062 ${swiss.nbPlayers}",
        s"092 Individual: Swiss-System",
        s"102 $baseUrl/swiss",
        s"XXR ${swiss.settings.nbRounds}",
        s"XXC ${Color.fromWhite(swiss.id.value(0).toInt % 2 == 0).name}1"
      )
    )

  private def playerLine(
      swiss: Swiss,
      playerIds: PlayerIds
  )(p: SwissPlayer, pairings: Map[SwissRoundNumber, SwissPairing], sheet: SwissSheet): Bits =
    List(
      3 -> "001",
      8 -> playerIds.getOrElse(p.userId, 0).toString,
      (15 + p.userId.value.size) -> p.userId.value,
      52 -> p.rating.toString,
      84 -> f"${sheet.points.value}%1.1f"
    ) ::: {
      swiss.allRounds.zip(sheet.outcomes).flatMap { case (rn, outcome) =>
        val pairing = pairings.get(rn)
        List(
          95 -> pairing.map(_.opponentOf(p.userId)).flatMap(playerIds.get).so(_.toString),
          97 -> pairing.map(_.colorOf(p.userId)).so(_.fold("w", "b")),
          99 -> {
            import SwissSheet.Outcome.*
            outcome match
              case Absent => "-"
              case Late => "H"
              case Bye => "U"
              case Draw => "="
              case Win => "1"
              case Loss => "0"
              case Ongoing => "Z"
              case ForfeitLoss => "-"
              case ForfeitWin => "+"
          }
        ).map { case (l, s) => (l + (rn.value - 1) * 10, s) }
      }
    } ::: {
      p.absent && swiss.round.value < swiss.settings.nbRounds
    }.so:
      List( // http://www.rrweb.org/javafo/aum/JaVaFo2_AUM.htm#_Unusual_info_extensions
        95 -> "0000",
        97 -> "",
        99 -> "-"
      ).map { case (l, s) => (l + swiss.round.value * 10, s) }

  private def formatLine(bits: Bits): String =
    bits.foldLeft("") { case (acc, (pos, txt)) =>
      s"""$acc${" " * (pos - txt.length - acc.length)}$txt"""
    }

  import java.time.format.{ DateTimeFormatter, FormatStyle }
  val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

  def fetchPlayerIds(swiss: Swiss): Fu[PlayerIds] =
    SwissPlayer.fields: p =>
      mongo.player
        .aggregateOne(): framework =>
          import framework.*
          Match($doc(p.swissId -> swiss.id)) -> List(
            Sort(Descending(p.rating)),
            Group(BSONNull)("us" -> PushField(p.userId))
          )
        .map:
          ~_.flatMap(_.getAsOpt[List[UserId]]("us"))
        .map:
          _.mapWithIndex: (userId, index) =>
            (userId, index + 1)
          .toMap

  private def forbiddenPairings(swiss: Swiss, playerIds: PlayerIds): Source[String, ?] =
    if swiss.settings.forbiddenPairings.isEmpty then Source.empty[String]
    else
      Source.fromIterator: () =>
        swiss.settings.forbiddenPairings.linesIterator.flatMap:
          _.trim.toLowerCase.split(' ').map(_.trim) match
            case Array(u1, u2) if u1 != u2 =>
              for
                id1 <- playerIds.get(UserId(u1))
                id2 <- playerIds.get(UserId(u2))
              yield s"XXP $id1 $id2"
            case _ => none
