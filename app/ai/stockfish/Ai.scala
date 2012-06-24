package lila
package ai
package stockfish

import chess.{ Game, Move, Rook }
import chess.format.UciDump
import chess.format.Forsyth
import game.DbGame

import akka.util.Timeout
import akka.util.Duration
import akka.util.duration._
import akka.dispatch.{ Future, Await }
import akka.actor.{ Props, Actor, ActorRef }
import akka.pattern.ask
import play.api.Play.current
import play.api.libs.concurrent._
import scalaz.effects._

final class Ai(execPath: String) extends lila.ai.Ai {

  import model._

  def apply(dbGame: DbGame, initialFen: Option[String]): IO[Valid[(Game, Move)]] = io {
    for {
      moves ← UciDump(dbGame.pgn, initialFen)
      play = Play(moves, initialFen map chess960Fen, dbGame.aiLevel | 1)
      bestMove ← unsafe {
        Await.result(actor ? play mapTo manifest[BestMove], atMost)
      }
      result ← bestMove.parse toValid "Invalid engine move"
      (orig, dest) = result
      result ← dbGame.toChess(orig, dest)
    } yield result
  }

  private def chess960Fen(fen: String) = (Forsyth << fen).fold(
    situation ⇒ fen.replace("KQkq", situation.board.pieces.toList filter {
      case (_, piece) ⇒ piece is Rook
    } sortBy {
      case (pos, _) ⇒ (pos.y, pos.x)
    } map {
      case (pos, piece) ⇒ piece.color.fold(pos.file.toUpperCase, pos.file)
    } mkString ""),
    fen)

  private val atMost = 5 seconds
  private implicit val timeout = new Timeout(atMost)

  private val process = Process(execPath) _
  private val config = new Config
  private val actor = Akka.system.actorOf(Props(new FSM(process, config)))
}
