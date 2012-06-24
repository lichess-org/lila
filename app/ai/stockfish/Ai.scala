package lila
package ai
package stockfish

import chess.{ Game, Move }
import chess.format.UciDump
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

  def apply(dbGame: DbGame): IO[Valid[(Game, Move)]] = io {
    for {
      moves ← UciDump(dbGame.pgn)
      bestMove ← unsafe {
        Await.result(actor ? Play(moves, None) mapTo manifest[BestMove], atMost)
      }
      result ← bestMove.parse toValid "Invalid engine move"
      (orig, dest) = result
      result ← dbGame.toChess(orig, dest)
    } yield result
  }

  private val atMost = 5 seconds
  private implicit val timeout = new Timeout(atMost)

  private val actor = Akka.system.actorOf(Props(new FSM(Process(execPath) _)))
}
