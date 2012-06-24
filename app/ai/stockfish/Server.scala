package lila
package ai
package stockfish

import chess.Rook
import chess.format.UciDump
import chess.format.Forsyth
import analyse.Analysis

import akka.util.Timeout
import akka.util.Duration
import akka.util.duration._
import akka.dispatch.{ Future, Await }
import akka.actor.{ Props, Actor, ActorRef }
import akka.pattern.ask
import play.api.Play.current
import play.api.libs.concurrent._
import scalaz.effects._

final class Server(
  execPath: String, 
  playConfig: PlayConfig,
  analyseConfig: AnalyseConfig) {

  def play(pgn: String, initialFen: Option[String], level: Int): Valid[IO[String]] = {
    implicit val timeout = new Timeout(playAtMost)
    if (level < 1 || level > 8) "Invalid ai level".failNel
    else for {
      moves ← UciDump(pgn, initialFen)
      play = model.play.Play(moves, initialFen map chess960Fen, level)
    } yield io {
      Await.result(playActor ? play mapTo manifest[model.play.BestMove], playAtMost)
    } map (_.move | "")
  }

  def analyse(pgn: String, initialFen: Option[String]): Valid[IO[Analysis]] = {
    implicit val timeout = new Timeout(analyseAtMost)
    for {
      moves ← UciDump(pgn, initialFen)
      analyse = model.analyse.Analyse(moves, initialFen map chess960Fen)
    } yield io {
      Await.result(analyseActor ? analyse mapTo manifest[Analysis], analyseAtMost)
    }
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

  private val playAtMost = 5 seconds
  private lazy val playProcess = Process(execPath) _
  private lazy val playActor = Akka.system.actorOf(Props(
    new PlayFSM(playProcess, playConfig)))

  private val analyseAtMost = 5 minutes
  private lazy val analyseProcess = Process(execPath) _
  private lazy val analyseActor = Akka.system.actorOf(Props(
    new AnalyseFSM(analyseProcess, analyseConfig)))
}
