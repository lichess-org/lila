package lila
package ai
package stockfish

import chess.Rook
import chess.format.UciDump
import chess.format.Forsyth
import analyse.Analysis
import model.{ GetQueueSize, QueueSize }

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

  def analyse(pgn: String, initialFen: Option[String]): Future[Valid[Analysis]] =
    UciDump(pgn, initialFen).fold(
      err ⇒ Future(failure(err)),
      moves ⇒ {
        val analyse = model.analyse.Analyse(moves, initialFen map chess960Fen)
        implicit val timeout = Timeout(1 hour)
        analyseActor ? analyse mapTo manifest[Valid[Analysis]]
      })

  def report = {
    implicit val timeout = new Timeout(playAtMost)
    (playActor ? GetQueueSize) zip (analyseActor ? GetQueueSize) map {
      case (QueueSize(play), QueueSize(analyse)) => play -> analyse
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

  private implicit val executor = Akka.system.dispatcher

  private val playAtMost = 10 seconds
  private lazy val playProcess = Process(execPath) _
  private lazy val playActor = Akka.system.actorOf(Props(
    new PlayFSM(playProcess, playConfig)))

  private lazy val analyseProcess = Process(execPath) _
  private lazy val analyseActor = Akka.system.actorOf(Props(
    new AnalyseFSM(analyseProcess, analyseConfig)))
}
