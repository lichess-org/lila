package lila.swiss

import akka.stream.scaladsl._
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.user.User

// https://www.fide.com/FIDE/handbook/C04Annex2_TRF16.pdf
final class SwissTrf(
    sheetApi: SwissSheetApi,
    colls: SwissColls,
    baseUrl: lila.common.config.BaseUrl
)(implicit ec: scala.concurrent.ExecutionContext) {

  private type Bits = List[(Int, String)]

  def apply(swiss: Swiss, sorted: Boolean): Source[String, _] = Source futureSource {
    fetchPlayerIds(swiss) map { apply(swiss, _, sorted) }
  }

  def apply(swiss: Swiss, playerIds: PlayerIds, sorted: Boolean): Source[String, _] =
    SwissPlayer.fields { f =>
      tournamentLines(swiss) concat
        forbiddenPairings(swiss, playerIds) concat sheetApi
          .source(swiss, sort = sorted.??($doc(f.rating -> -1)))
          .map((playerLine(swiss, playerIds) _).tupled)
          .map(formatLine)
    }

  private def tournamentLines(swiss: Swiss) =
    Source(
      List(
        s"012 ${swiss.name}",
        s"022 $baseUrl/swiss/${swiss.id}",
        s"032 Lichess",
        s"042 ${dateFormatter print swiss.startsAt}",
        s"052 ${swiss.finishedAt ?? dateFormatter.print}",
        s"062 ${swiss.nbPlayers}",
        s"092 Individual: Swiss-System",
        s"102 $baseUrl/swiss",
        s"XXR ${swiss.settings.nbRounds}",
        s"XXC ${chess.Color.fromWhite(swiss.id.value(0).toInt % 2 == 0).name}1"
      )
    )

  private def playerLine(
      swiss: Swiss,
      playerIds: PlayerIds
  )(p: SwissPlayer, pairings: Map[SwissRound.Number, SwissPairing], sheet: SwissSheet): Bits =
    List(
      3                    -> "001",
      8                    -> playerIds.getOrElse(p.userId, 0).toString,
      (15 + p.userId.size) -> p.userId,
      52                   -> p.rating.toString,
      84                   -> f"${sheet.points.value}%1.1f"
    ) ::: {
      swiss.allRounds.zip(sheet.outcomes).flatMap { case (rn, outcome) =>
        val pairing = pairings get rn
        List(
          95 -> pairing.map(_ opponentOf p.userId).flatMap(playerIds.get).??(_.toString),
          97 -> pairing.map(_ colorOf p.userId).??(_.fold("w", "b")),
          99 -> {
            import SwissSheet._
            outcome match {
              case Absent  => "-"
              case Late    => "H"
              case Bye     => "U"
              case Draw    => "="
              case Win     => "1"
              case Loss    => "0"
              case Ongoing => "Z"
            }
          }
        ).map { case (l, s) => (l + (rn.value - 1) * 10, s) }
      }
    } ::: {
      p.absent && swiss.round.value < swiss.settings.nbRounds
    }.?? {
      List( // http://www.rrweb.org/javafo/aum/JaVaFo2_AUM.htm#_Unusual_info_extensions
        95 -> "0000",
        97 -> "",
        99 -> "-"
      ).map { case (l, s) => (l + swiss.round.value * 10, s) }
    }

  private def formatLine(bits: Bits): String =
    bits.foldLeft("") { case (acc, (pos, txt)) =>
      s"""$acc${" " * (pos - txt.length - acc.length)}$txt"""
    }

  private val dateFormatter = org.joda.time.format.DateTimeFormat forStyle "M-"

  def fetchPlayerIds(swiss: Swiss): Fu[PlayerIds] =
    SwissPlayer
      .fields { p =>
        import BsonHandlers._
        colls.player
          .aggregateOne() { framework =>
            import framework._
            Match($doc(p.swissId -> swiss.id)) -> List(
              Sort(Descending(p.rating)),
              Group(BSONNull)("us" -> PushField(p.userId))
            )
          }
          .map {
            ~_.flatMap(_.getAsOpt[List[User.ID]]("us"))
          }
          .map {
            _.view.zipWithIndex.map { case (userId, index) =>
              (userId, index + 1)
            }.toMap
          }
      }

  private def forbiddenPairings(swiss: Swiss, playerIds: PlayerIds): Source[String, _] =
    if (swiss.settings.forbiddenPairings.isEmpty) Source.empty[String]
    else
      Source.fromIterator { () =>
        swiss.settings.forbiddenPairings.linesIterator.flatMap {
          _.trim.toLowerCase.split(' ').map(_.trim) match {
            case Array(u1, u2) if u1 != u2 =>
              for {
                id1 <- playerIds.get(u1)
                id2 <- playerIds.get(u2)
              } yield s"XXP $id1 $id2"
            case _ => none
          }
        }
      }
}
