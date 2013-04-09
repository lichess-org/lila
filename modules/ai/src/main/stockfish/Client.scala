package lila.ai
package stockfish

import chess.{ Game, Move }
import chess.format.UciMove
import lila.analyse.{ Analysis, AnalysisMaker }

import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WS

final class Client(
    val playUrl: String,
    analyseUrl: String) extends lila.ai.Client {

  def play(game: Game, pgn: String, initialFen: Option[String], level: Int): Fu[Valid[(Game, Move)]] =
    fetchMove(pgn, ~initialFen, level) map { Stockfish.applyMove(game, pgn, _) }

  def analyse(pgn: String, initialFen: Option[String]): Fu[Valid[String ⇒ Analysis]] =
    fetchAnalyse(pgn, ~initialFen) map {
      AnalysisMaker(_, true) toValid "Can't read analysis results"
    } recover {
      case e ⇒ !![String ⇒ Analysis](e.getMessage)
    }

  protected def tryPing: Fu[Int] = nowMillis |> { start ⇒
    fetchMove(pgn = "", initialFen = "", level = 1) map {
      case move if UciMove(move).isDefined ⇒ (nowMillis - start).toInt
    }
  }

  private def fetchMove(pgn: String, initialFen: String, level: Int): Fu[String] =
    WS.url(playUrl).withQueryString(
      "pgn" -> pgn,
      "initialFen" -> initialFen,
      "level" -> level.toString
    ).get() map (_.body)

  private def fetchAnalyse(pgn: String, initialFen: String): Fu[String] =
    WS.url(analyseUrl).withQueryString(
      "pgn" -> pgn,
      "initialFen" -> initialFen
    ).get() map (_.body)
}
