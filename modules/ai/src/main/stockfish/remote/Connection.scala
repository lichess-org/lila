package lila.ai
package stockfish
package remote

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._
import lila.common.ws.WS
import lila.hub.actorApi.ai.GetLoad

private[ai] final class Connection(
    name: String,
    config: Config,
    router: Router) extends Actor {

  private var load = none[Int]

  def receive = {

    case GetLoad ⇒ sender ! load

    case CalculateLoad ⇒ Future {
      Try(WS.url(router.load).get() map (_.body) map parseIntOption await makeTimeout(config.loadTimeout)) match {
        case Success(l) ⇒ load = l
        case Failure(e) ⇒ load = none
      }
    }
    case Play(uciMoves, fen, level) ⇒ WS.url(router.play).withQueryString(
      "uciMoves" -> uciMoves,
      "initialFen" -> fen,
      "level" -> level.toString
    ).get() map (_.body) pipeTo sender

    case Analyse(uciMoves: String, fen: String) ⇒ WS.url(router.analyse).withQueryString(
      "uciMoves" -> uciMoves,
      "initialFen" -> fen
    ).get() map (_.body) pipeTo sender
  }

}
