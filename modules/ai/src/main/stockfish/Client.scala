package lila.ai
package stockfish

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.concurrent._
import play.api.libs.ws.WS
import play.api.Play.current

import actorApi.monitor._
import chess.format.UciMove
import chess.{ Game, Move }
import lila.analyse.AnalysisMaker
import lila.hub.actorApi.ai.GetLoad

final class Client(
    val playUrl: String,
    analyseUrl: String,
    loadUrl: String,
    system: ActorSystem) extends lila.ai.Ai {

  def play(game: Game, pgn: String, initialFen: Option[String], level: Int): Fu[(Game, Move)] =
    fetchMove(pgn, ~initialFen, level) flatMap { Stockfish.applyMove(game, pgn, _) }

  def analyse(pgn: String, initialFen: Option[String]): Fu[AnalysisMaker] =
    fetchAnalyse(pgn, ~initialFen) flatMap { str ⇒
      (AnalysisMaker(str, true) toValid "Can't read analysis results").future
    }

  def load: Fu[Option[Int]] = {
    import makeTimeout.short
    actor ? GetLoad mapTo manifest[Option[Int]]
  }

  private val actor = system.actorOf(Props(new Actor {

    private var load = none[Int]

    def receive = {
      case IsHealthy ⇒ sender ! load.isDefined
      case GetLoad   ⇒ sender ! load
      case CalculateLoad ⇒ try {
        load = fetchLoad await makeTimeout.short
      }
      catch {
        case e: Exception ⇒ {
          logwarn("[stockfish client] " + e.getMessage)
          load = none
        }
      }
    }
  }))
  system.scheduler.schedule(1.millis, 1.second, actor, CalculateLoad)

  def or(fallback: lila.ai.Ai): Fu[lila.ai.Ai] = {
    import makeTimeout.short
    actor ? IsHealthy mapTo manifest[Boolean] map {
      _.fold(this, fallback)
    }
  }

  private def fetchMove(pgn: String, initialFen: String, level: Int): Fu[String] =
    WS.url(playUrl).withQueryString(
      "pgn" -> pgn,
      "initialFen" -> initialFen,
      "level" -> level.toString
    ).get() map (_.body)

  private def fetchAnalyse(pgn: String, initialFen: String): Fu[String] =
    WS.url(analyseUrl).withRequestTimeout(10000 /* ms */).withQueryString(
      "pgn" -> pgn,
      "initialFen" -> initialFen
    ).get() map (_.body)

  private def fetchLoad: Fu[Option[Int]] =
    WS.url(loadUrl).get() map (_.body) map parseIntOption
}
