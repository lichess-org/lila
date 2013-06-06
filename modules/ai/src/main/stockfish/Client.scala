package lila.ai
package stockfish

import scala.concurrent.duration._

import play.api.libs.concurrent._
import play.api.libs.ws.WS
import play.api.Play.current

import chess.format.UciMove
import chess.{ Game, Move }
import lila.analyse.AnalysisMaker

final class Client(
    val playUrl: String,
    analyseUrl: String) extends lila.ai.Client {

  def play(game: Game, pgn: String, initialFen: Option[String], level: Int): Fu[(Game, Move)] =
    fetchMove(pgn, ~initialFen, level) flatMap { Stockfish.applyMove(game, pgn, _) }

  def analyse(pgn: String, initialFen: Option[String]): Fu[AnalysisMaker] =
    fetchAnalyse(pgn, ~initialFen) flatMap { str ⇒
      (AnalysisMaker(str, true) toValid "Can't read analysis results").future
    }

  protected def tryPing: Fu[Int] = nowMillis |> { start ⇒
    fetchMove(pgn = "", initialFen = "", level = 1) flatMap {
      case move if UciMove(move).isDefined ⇒ fuccess((nowMillis - start).toInt)
      case move                            ⇒ fufail("[stockfish] client invalid ping response " + move)
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
