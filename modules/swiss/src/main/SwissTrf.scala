package lila.swiss

import lila.db.dsl._

final class SwissTrf(
    colls: SwissColls
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._
  private type Bits = List[(Int, String)]

  def apply(swiss: Swiss): Fu[String] =
    fetchData(swiss) map {
      case (players, pairings) => apply(swiss, players, pairings)
    }

  def apply(swiss: Swiss, players: List[SwissPlayer], pairings: List[SwissPairing]): String = {
    s"XXR ${swiss.settings.nbRounds}" ::
      s"XXC ${chess.Color(scala.util.Random.nextBoolean).name}1" ::
      players.map(player(swiss, SwissPairing.toMap(pairings))).map(format)
  } mkString "\n"

  def fetchData(swiss: Swiss): Fu[(List[SwissPlayer], List[SwissPairing])] =
    SwissPlayer.fields { f =>
      colls.player.ext
        .find($doc(f.swissId -> swiss.id))
        .sort($sort asc f.number)
        .list[SwissPlayer]()
    } zip
      SwissPairing.fields { f =>
        colls.pairing.ext
          .find($doc(f.swissId -> swiss.id))
          .sort($sort asc f.round)
          .list[SwissPairing]()
      }

  // https://www.fide.com/FIDE/handbook/C04Annex2_TRF16.pdf
  private def player(swiss: Swiss, pairingMap: SwissPairing.PairingMap)(p: SwissPlayer): Bits = {
    val sheet = SwissSheet.one(swiss, ~pairingMap.get(p.number), p)
    List(
      3  -> "001",
      8  -> p.number.toString,
      47 -> p.userId,
      84 -> f"${sheet.points.value}%1.1f"
    ) ::: {
      val pairings = ~pairingMap.get(p.number)
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
  }

  private def format(bits: Bits): String =
    bits.foldLeft("") {
      case (acc, (pos, txt)) => acc + (" " * (pos - txt.size - acc.size)) + txt
    }
}
