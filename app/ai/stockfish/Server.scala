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
import akka.actor.{ Props, Actor, ActorRef, Kill }
import akka.pattern.{ ask, AskTimeoutException }
import play.api.Play.current
import play.api.libs.concurrent._
import scalaz.effects._

final class Server(
    execPath: String,
    playConfig: PlayConfig,
    analyseConfig: AnalyseConfig) {

  def play(pgn: String, initialFen: Option[String], level: Int): Future[Valid[String]] = {
    implicit val timeout = new Timeout(playAtMost)
    (for {
      moves ← UciDump(pgn, initialFen)
      play = model.play.Play(moves, initialFen map chess960Fen, level, playConfig)
    } yield play).fold(
      err ⇒ Future(failure(err)),
      play ⇒ playActor ? play mapTo bestMoveManifest map { m ⇒
        success(m.move | "")
      } onFailure reboot(playActor)
    )
  }

  def analyse(pgn: String, initialFen: Option[String]): Future[Valid[Analysis]] =
    UciDump(pgn, initialFen).fold(
      err ⇒ Future(failure(err)),
      moves ⇒ {
        val analyse = model.analyse.Analyse(moves, initialFen map chess960Fen)
        implicit val timeout = Timeout(1 hour)
        analyseActor ? analyse mapTo analysisManifest onFailure reboot(analyseActor)
      }
    )

  def report: Future[(Int, Int)] = {
    implicit val timeout = new Timeout(playAtMost)
    (playActor ? GetQueueSize) zip (analyseActor ? GetQueueSize) map {
      case (QueueSize(play), QueueSize(analyse)) ⇒ play -> analyse
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

  private def reboot(actor: ActorRef): PartialFunction[Throwable, Unit] = {
    case e: AskTimeoutException ⇒ actor ! model.RebootException
  }
  private val analysisManifest = manifest[Valid[Analysis]]
  private val bestMoveManifest = manifest[model.play.BestMove]

  private implicit val executor = Akka.system.dispatcher

  private val playAtMost = 5 seconds
  private lazy val playProcess = Process(execPath, "SFP") _
  private lazy val playActor = Akka.system.actorOf(Props(
    new PlayFSM(playProcess, playConfig)))

  private lazy val analyseProcess = Process(execPath, "SFA") _
  private lazy val analyseActor = Akka.system.actorOf(Props(
    new AnalyseFSM(analyseProcess, analyseConfig)))
}
