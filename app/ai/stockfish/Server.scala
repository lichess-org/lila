package lila
package ai
package stockfish

import chess.Rook
import chess.format.UciDump
import chess.format.Forsyth

import akka.util.Timeout
import akka.util.Duration
import akka.util.duration._
import akka.dispatch.{ Future, Await }
import akka.actor.{ Props, Actor, ActorRef }
import akka.pattern.ask
import play.api.Play.current
import play.api.libs.concurrent._
import scalaz.effects._

final class Server(execPath: String, config: Config) {

  import model._

  def apply(pgn: String, initialFen: Option[String], level: Int): Valid[IO[String]] =
    if (level < 1 || level > 8) "Invalid ai level".failNel
    else for {
      moves ← UciDump(pgn, initialFen)
      play = Play(moves, initialFen map chess960Fen, level)
    } yield io {
      Await.result(actor ? play mapTo manifest[BestMove], atMost)
    } map (_.move | "")

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
  private val actor = Akka.system.actorOf(Props(new FSM(process, config)))
}
