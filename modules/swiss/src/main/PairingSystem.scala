package lila.swiss

import akka.stream.scaladsl.*
import akka.util.ByteString

import java.io.File
import scala.concurrent.blocking
import scala.sys.process.*

final private class PairingSystem(trf: SwissTrf, executable: String)(using
    Executor,
    akka.stream.Materializer
):

  def apply(swiss: Swiss): Fu[List[SwissPairing.ByeOrPending]] =
    trf.fetchPlayerIds(swiss).flatMap { playerIds =>
      invoke(swiss, trf(swiss, playerIds, sorted = false)).map:
        reader(playerIds.map(_.swap), _)
    }

  private def invoke(swiss: Swiss, input: Source[String, ?]): Fu[List[String]] =
    withTempFile(swiss, input) { file =>
      val flavour =
        if swiss.nbPlayers < 250 then "dutch"
        else if swiss.nbPlayers < 700 then "burstein"
        else "fast"
      val command = s"$executable --$flavour $file -p"
      val stdout = new collection.mutable.ListBuffer[String]
      val stderr = new StringBuilder
      val status = lila.common.Chronometer.syncMon(_.swiss.bbpairing):
        blocking:
          command ! ProcessLogger(stdout append _, stderr append _)
      if status != 0 then
        val error = stderr.toString
        if error.contains("No valid pairing exists") then Nil
        else throw PairingSystem.BBPairingException(error, swiss)
      else stdout.toList
    }

  private def reader(idsToPlayers: IdPlayers, output: List[String]): List[SwissPairing.ByeOrPending] =
    output
      .drop(1) // first line is the number of pairings
      .map(_.split(' '))
      .collect:
        case Array(p, "0") =>
          p.toIntOption.flatMap(idsToPlayers.get).map { userId =>
            Left(SwissPairing.Bye(userId))
          }
        case Array(w, b) =>
          for
            white <- w.toIntOption.flatMap(idsToPlayers.get)
            black <- b.toIntOption.flatMap(idsToPlayers.get)
          yield Right(SwissPairing.Pending(white, black))
      .flatten

  def withTempFile[A](swiss: Swiss, contents: Source[String, ?])(f: File => A): Fu[A] =
    // NOTE: The prefix and suffix must be at least 3 characters long,
    // otherwise this function throws an IllegalArgumentException.
    val file = File.createTempFile(s"lila-swiss-${swiss.id}-${swiss.round}-", s"-bbp")
    contents
      .intersperse("\n")
      .map(ByteString.apply)
      .runWith(FileIO.toPath(file.toPath))
      .map { _ =>
        val res = f(file)
        file.delete()
        res
      }

private object PairingSystem:
  case class BBPairingException(message: String, swiss: Swiss) extends lila.core.lilaism.LilaException
