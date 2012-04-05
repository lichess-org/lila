package lila.system
package ai

import lila.chess.{ Game, Move }
import model._

import java.io.File
import scala.io.Source
import scala.sys.process.Process
import scalaz.effects._

final class CraftyServer(
    execPath: String,
    bookPath: Option[String] = None) {

  def apply(fen: String, level: Int): Valid[IO[String]] =
    if (level < 1 || level > 8) "Invalid ai level".failNel
    else if (fen.isEmpty) "Empty fen".failNel
    else success(runCrafty(fen, level))

  def runCrafty(oldFen: String, level: Int): IO[String] = for {
    file ← writeFile("lichess_crafty_", input(oldFen, level))
    output ← io { Process(command(level)) #< file !! }
  } yield extractFen(output)

  private def extractFen(output: String) = {
    output.lines.find(_ contains "setboard") map { line ⇒
      """^.+setboard\s([\w\d/]+\s\w).*$""".r.replaceAllIn(line, m ⇒ m group 1)
    } getOrElse "Crafty output does not contain setboard"
  }

  private def command(level: Int) =
    """%s learn=off log=off bookpath=%s ponder=off smpmt=1 st=%s""".format(
      execPath,
      bookPath | "",
      craftyTime(level))

  private def input(fen: String, level: Int) = List(
    "skill %d" format craftySkill(level),
    "book random 1",
    "book width 10",
    "setboard %s" format fen,
    "move",
    "savepos",
    "quit")

  private def craftyTime(level: Int) = (level / 10f).toString take 4

  private def craftySkill(level: Int) = level match {
    case 8 ⇒ 100
    case l ⇒ l * 12
  }

  private def writeFile(prefix: String, data: List[String]): IO[File] = io {
    File.createTempFile(prefix, ".tmp") ~ { file ⇒
      try {
        file.deleteOnExit
      }
      catch {
        case e ⇒ println("Error deleting crafty file on exit: " + e.getMessage)
      }
      printToFile(file)(p ⇒ data foreach p.println)
    }
  }

  private def printToFile(f: java.io.File)(op: java.io.PrintWriter ⇒ Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }
}
