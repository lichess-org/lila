package lila
package ai

import chess.{ Game, Move, ReverseEngineering }
import chess.format.Forsyth
import model._

import java.io.File
import scala.io.Source
import scala.sys.process.Process
import scalaz.effects._

final class CraftyAi(
    execPath: String,
    bookPath: Option[String] = None) extends Ai {

  def apply(dbGame: DbGame): IO[Valid[(Game, Move)]] = {

    val oldGame = dbGame.toChess

    val forsyth = Forsyth >> (dbGame.variant match {
      case Chess960 ⇒ oldGame updateBoard { board ⇒
        board updateHistory (_.withoutAnyCastles)
      }
      case _ ⇒ oldGame
    })

    runCrafty(forsyth, dbGame.aiLevel | 1) map { newFen ⇒
      for {
        newSituation ← Forsyth << newFen toValid "Cannot parse engine FEN: " + newFen
        reverseEngineer = new ReverseEngineering(oldGame, newSituation.board)
        poss = reverseEngineer.move.mapFail(msgs ⇒
          (dbGame.id + " ReverseEngineering failure: " + (msgs.list mkString "\n") + "\n--------\n" + oldGame.board + "\n" + newSituation.board + "\n" + forsyth + "\n" + newFen).wrapNel
        ).err
        (orig, dest) = poss
        newGameAndMove ← oldGame(orig, dest)
      } yield newGameAndMove
    }
  }

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
