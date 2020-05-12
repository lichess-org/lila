package lila.swiss

import java.io.{ File, PrintWriter }
import scala.util.chaining._
import scala.sys.process._
import scala.concurrent.blocking

final private class PairingSystem(trf: SwissTrf, executable: String) {

  def apply(
      swiss: Swiss,
      players: List[SwissPlayer],
      pairings: List[SwissPairing]
  ): List[SwissPairing.ByeOrPending] =
    trf(swiss, players, pairings) pipe invoke pipe reader

  private def invoke(input: String): List[String] =
    lila.mon.chronoSync(_.swiss.bbpairing) {
      blocking {
        withTempFile(input) { file =>
          val command = s"$executable --dutch $file -p"
          val stdout  = new collection.mutable.ListBuffer[String]
          val stderr  = new StringBuilder
          val status  = command ! ProcessLogger(stdout append _, stderr append _)
          if (status != 0) {
            val error = stderr.toString
            if (error contains "No valid pairing exists") Nil
            else throw new PairingSystem.BBPairingException(error, input)
          } else stdout.toList
        }
      }
    }

  private def reader(output: List[String]): List[SwissPairing.ByeOrPending] =
    output
      .drop(1) // first line is the number of pairings
      .map(_ split ' ')
      .collect {
        case Array(p, "0") =>
          p.toIntOption map { p =>
            Left(SwissPairing.Bye(SwissPlayer.Number(p)))
          }
        case Array(w, b) =>
          for {
            white <- w.toIntOption
            black <- b.toIntOption
          } yield Right(SwissPairing.Pending(SwissPlayer.Number(white), SwissPlayer.Number(black)))
      }
      .flatten

  /** NOTE: This function uses the createTempFile function from the File class. The prefix and
    * suffix must be at least 3 characters long, otherwise this function throws an IllegalArgumentException.
    */
  def withTempFile[A](contents: String)(f: File => A): A = {
    val file = File.createTempFile("lila-", "-swiss")
    val p    = new PrintWriter(file, "UTF-8")
    try {
      p.write(contents)
      p.flush()
      val res = f(file)
      res
    } finally {
      p.close()
      file.delete()
    }
  }
}

private object PairingSystem {
  case class BBPairingException(val message: String, val input: String) extends lila.base.LilaException
}
