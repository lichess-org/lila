package lila
package ai
package crafty

import chess.{ Game, Move }

import java.io.ByteArrayInputStream
import scala.io.Source
import scala.sys.process.Process
import scalaz.effects._

final class Server(
    execPath: String,
    bookPath: Option[String] = None) extends lila.ai.Server {

  def play(fen: String, level: Int): Valid[IO[String]] = for {
    validFen ← fen.validIf(fen.nonEmpty, "Empty FEN string")
    validLevel ← validateLevel(level)
  } yield io { 
    Process(command(validLevel)) #< input(validFen, validLevel) !! 
  } map extractFen

  private def extractFen(output: String) =
    output.lines.find(_ contains "setboard") map { line ⇒
      """^.+setboard\s([\w\d/]+\s\w).*$""".r.replaceAllIn(line, m ⇒ m group 1)
    } getOrElse "Crafty output does not contain setboard"

  private def command(level: Int) =
    """%s learn=off log=off bookpath=%s ponder=off smpmt=1 st=%s""".format(
      execPath,
      bookPath | "",
      craftyTime(level))

  private def input(fen: String, level: Int) = new ByteArrayInputStream(List(
    "skill %d" format craftySkill(level),
    "book random 1",
    "book width 10",
    "setboard %s" format fen,
    "move",
    "savepos",
    "quit") mkString "\n" getBytes "UTF-8")

  private def craftyTime(level: Int) = (level / 10f).toString take 4

  private def craftySkill(level: Int) = level match {
    case 8 ⇒ 100
    case l ⇒ l * 12
  }
}
