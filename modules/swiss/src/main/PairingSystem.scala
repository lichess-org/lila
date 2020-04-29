package lila.swiss

import java.io.{ File, PrintWriter }
import scala.util.chaining._

final private class PairingSystem(executable: String) {

  def apply(
      swiss: Swiss,
      players: List[SwissPlayer],
      pairings: List[SwissPairing]
  ): List[SwissPairing.Pending] =
    writer(swiss, players, pairings) pipe invoke pipe reader

  private def invoke(input: String): String =
    withTempFile(input) { file =>
      s"$executable --dutch $file -p"
    }

  private def reader(output: String): List[SwissPairing.Pending] =
    output.linesIterator.toList
      .map(_ split ' ')
      .collect {
        case Array(w, b) =>
          for {
            white <- w.toIntOption
            black <- b.toIntOption
          } yield SwissPairing.Pending(SwissPlayer.Number(white), SwissPlayer.Number(black))
      }
      .flatten

  private object writer {

    private type Bits       = List[(Int, String)]
    private type PairingMap = Map[SwissPlayer.Number, Map[Int, SwissPairing]]

    def apply(swiss: Swiss, players: List[SwissPlayer], pairings: List[SwissPairing]): String = {
      val pairingMap: PairingMap = pairings.foldLeft[PairingMap](Map.empty) {
        case (acc, pairing) =>
          pairing.players.foldLeft(acc) {
            case (acc, player) =>
              acc.updatedWith(player) { acc =>
                (~acc).updated(pairing.round.value, pairing).some
              }
          }
      }
      s"XXR ${swiss.nbRounds}" :: players.map(player(pairingMap, swiss.round)).map(format)
    } mkString "\n"

    // https://www.fide.com/FIDE/handbook/C04Annex2_TRF16.pdf
    private def player(pairingMap: PairingMap, rounds: SwissRound.Number)(p: SwissPlayer): Bits =
      List(
        3  -> "001",
        8  -> p.number.toString,
        84 -> f"${p.points.value}%1.1f"
      ) ::: {
        val pairings = ~pairingMap.get(p.number)
        (1 to rounds.value).toList.flatMap { rn =>
          val pairing = pairings get rn
          List(
            95 -> pairing.map(_ opponentOf p.number).??(_.toString),
            97 -> pairing.map(_ colorOf p.number).??(_.fold("w", "n")),
            99 -> pairing.flatMap(_.winner).map(p.number ==).fold("=") {
              case true  => "1"
              case false => "0"
            }
          ).map { case (l, s) => (l + (rn - 1) * 10, s) }
        }
      }

    private def format(bits: Bits): String = bits.foldLeft("") {
      case (acc, (pos, txt)) => acc + (" " * (pos - txt.size - acc.size)) + txt
    }
  }

  /** NOTE: This function uses the createTempFile function from the File class. The prefix and
    * suffix must be at least 3 characters long, otherwise this function throws an IllegalArgumentException.
    */
  def withTempFile[A](contents: String)(f: File => A): A = {
    val file = File.createTempFile("lila-", "-swiss")
    val p    = new PrintWriter(file, "UTF-8")
    try {
      p.write(contents)
      val res = f(file)
      file.delete()
      res
    } finally {
      p.close()
    }
  }
}
