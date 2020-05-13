package lila.swiss

import akka.stream.scaladsl._
import akka.util.ByteString
import java.io.{ File, PrintWriter }
import scala.concurrent.blocking
import scala.sys.process._

final private class PairingSystem(trf: SwissTrf, executable: String)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  def apply(swiss: Swiss): Fu[List[SwissPairing.ByeOrPending]] =
    invoke(swiss, trf(swiss)) map reader

  private def invoke(swiss: Swiss, input: Source[String, _]): Fu[List[String]] =
    withTempFile(input) { file =>
      val command = s"$executable --dutch $file -p"
      val stdout  = new collection.mutable.ListBuffer[String]
      val stderr  = new StringBuilder
      val status = blocking {
        command ! ProcessLogger(stdout append _, stderr append _)
      }
      if (status != 0) {
        val error = stderr.toString
        if (error contains "No valid pairing exists") Nil
        else throw new PairingSystem.BBPairingException(error, swiss)
      } else stdout.toList
    }.mon(_.swiss.bbpairing)

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
  def withTempFile[A](contents: Source[String, _])(f: File => A): Fu[A] = {
    val file = File.createTempFile("lila-", "-swiss")
    contents
      .intersperse("\n")
      .map(ByteString.apply)
      .runWith(FileIO.toPath(file.toPath))
      .map { _ =>
        val res = f(file)
        file.delete()
        res
      }
  }
}

private object PairingSystem {
  case class BBPairingException(val message: String, val swiss: Swiss) extends lila.base.LilaException
}
