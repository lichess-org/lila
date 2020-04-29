package lila.swiss

import java.io.{ File, PrintWriter }
import scala.util.chaining._

final private class PairingSystem(executable: String) {

  def apply(
      swiss: Swiss,
      players: List[SwissPlayer],
      rounds: List[SwissRound]
  ): List[SwissPairing.Pending] =
    writer(swiss, players, rounds) pipe invoke pipe reader

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

    private type Bits = List[(Int, String)]

    def apply(swiss: Swiss, players: List[SwissPlayer], rounds: List[SwissRound]): String = {
      s"XXR ${swiss.nbRounds}" :: players.map(player(rounds)).map(format)
    } mkString "\n"

    // https://www.fide.com/FIDE/handbook/C04Annex2_TRF16.pdf
    private def player(rounds: List[SwissRound])(p: SwissPlayer): Bits =
      List(
        3  -> "001",
        8  -> p.number.toString,
        84 -> f"${p.points.value}%1.1f"
      ) ::: rounds.flatMap { r =>
        val pairing = r.pairingsMap.get(p.number)
        List(
          95 -> pairing.map(_ opponentOf p.number).??(_.toString),
          97 -> pairing.map(_ colorOf p.number).??(_.fold("w", "n")),
          99 -> pairing.flatMap(_.winner).map(p.number ==).fold("=") {
            case true  => "1"
            case false => "0"
          }
        ).map { case (l, s) => (l + (r.number.value - 1) * 10, s) }
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
