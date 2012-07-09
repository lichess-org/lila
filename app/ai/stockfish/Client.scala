package lila
package ai
package stockfish

import chess.{ Game, Move }
import chess.format.UciMove
import game.DbGame
import analyse.Analysis

import scalaz.effects._
import akka.dispatch.{ Future, Await }
import akka.util.duration._
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.ws.WS

final class Client(
    val playUrl: String,
    analyseUrl: String) extends ai.Client with Stockfish {

  def play(dbGame: DbGame, initialFen: Option[String]): Future[Valid[(Game, Move)]] = {
    fetchMove(dbGame.pgn, initialFen | "", dbGame.aiLevel | 1) map {
      applyMove(dbGame, _)
    }
  }

  def analyse(dbGame: DbGame, initialFen: Option[String]): Future[Valid[Analysis]] =
    fetchAnalyse(dbGame.pgn, initialFen | "") map {
      Analysis(_, true)
    } recover {
      case e ⇒ !![Analysis](e.getMessage)
    }

  protected lazy val tryPing: Option[Int] = nowMillis |> { start ⇒
    (unsafe {
      Await.result(fetchMove(pgn = "", initialFen = "", level = 1), 5 seconds)
    }).toOption flatMap {
      case move if UciMove(move).isDefined ⇒ Some(nowMillis - start) map (_.toInt)
      case _                               ⇒ None
    }
  }

  private def fetchMove(pgn: String, initialFen: String, level: Int): Future[String] =
    toAkkaFuture(WS.url(playUrl).post(Map(
      "pgn" -> Seq(pgn),
      "initialFen" -> Seq(initialFen),
      "level" -> Seq(level.toString)
    )) map (_.body))

  private def fetchAnalyse(pgn: String, initialFen: String): Future[String] =
    toAkkaFuture(WS.url(analyseUrl).post(Map(
      "pgn" -> Seq(pgn),
      "initialFen" -> Seq(initialFen)
    )) map (_.body))

  private implicit val executor = Akka.system.dispatcher

  private def toAkkaFuture[A](promise: Promise[A]): Future[A] = {
    val p = akka.dispatch.Promise[A]()
    promise extend1 {
      case Redeemed(value) ⇒ p success value
      case Thrown(exn)     ⇒ p failure exn
    }
    p.future
  }
}
