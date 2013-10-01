package lila.ai
package stockfish

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.format.UciMove
import chess.{ Game, Move }
import play.api.libs.concurrent._
import play.api.Play.current
import remote.actorApi._

import lila.analyse.AnalysisMaker
import lila.hub.actorApi.ai.GetLoad

final class Client(
    dispatcher: ActorRef,
    fallback: lila.ai.Ai) extends lila.ai.Ai {

  private implicit val timeout = makeTimeout minutes 60

  def play(game: Game, pgn: String, initialFen: Option[String], level: Int): Fu[(Game, Move)] =
    dispatcher ? Play(pgn, ~initialFen, level) mapTo manifest[String] flatMap {
      Stockfish.applyMove(game, pgn, _)
    } recoverWith {
      case e: Exception ⇒ fallback.play(game, pgn, initialFen, level)
    }

  def analyse(pgn: String, initialFen: Option[String]): Fu[AnalysisMaker] =
    dispatcher ? Analyse(pgn, ~initialFen) mapTo manifest[String] flatMap { str ⇒
      (AnalysisMaker(str, true) toValid "Can't read analysis results").future
    } recoverWith {
      case e: Exception ⇒ fallback.analyse(pgn, initialFen)
    }

  def load: Fu[List[Option[Int]]] =
    dispatcher ? GetLoad mapTo manifest[List[Option[Int]]]
}
