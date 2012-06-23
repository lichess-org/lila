package lila
package ai

import chess.{ Game, Move }
import chess.format.UciDump
import game.DbGame

import akka.util.Timeout
import akka.util.Duration
import akka.util.duration._
import akka.dispatch.{ Future, Await }
import akka.actor.{ Props, Actor }
import akka.pattern.ask
import play.api.Play.current
import play.api.libs.concurrent._
import scalaz.effects._

final class StockfishAi(execPath: String) extends Ai {

  def apply(dbGame: DbGame): IO[Valid[(Game, Move)]] = io {
    UciDump(dbGame.pgn) map { uci =>
      val move: String = Await.result(
        actor ? BestMove(None, uci) mapTo manifest[String],
        atMost)
    }
    !!("haha")
  }

  private val atMost = 5 seconds
  private implicit val timeout = Timeout(atMost)

  private val actor = Akka.system.actorOf(Props(new Actor {
    import java.io.{ InputStream, InputStreamReader, OutputStream }
    import scala.sys.process.{ Process, ProcessIO }
    import scala.io.Source.fromInputStream

    var in: OutputStream = _
    var out: InputStream = _
    var err: InputStream = _
    val processBuilder = Process(execPath)
    val processIO = new ProcessIO(
      i ⇒ { in = i },
      o ⇒ sendLines(fromInputStream(o).getLines, Out(_)),
      e ⇒ sendLines(fromInputStream(e).getLines, Err(_)))
    val process = processBuilder run processIO

    def receive = {
      case BestMove(fen, moves) ⇒ {
        write("ucinewgame")
        write("isready")
        write("position " + moves)
        write("go movetime 500")
      }
      case Out(msg) => println(msg)
      case Err(msg) => println("ERR " + msg)
    }

    def sendLines(lines: Iterator[String], as: String ⇒ Msg) {
      lines foreach { l ⇒ self ! as(l) }
    }
    def write(txt: String) {
      in write txt.pp.getBytes("UTF-8")
    }
  }))

  case class BestMove(fen: Option[String], moves: String)

  sealed trait Msg {
    def text: String
  }
  case class Out(text: String) extends Msg
  case class Err(text: String) extends Msg
}
