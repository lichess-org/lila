package lila.ai
package stockfish
package remote

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._
import lila.common.ws.WS
import lila.hub.actorApi.ai.GetLoad

private[ai] final class Connection(router: Router) extends Actor {

  private var load = none[Int]

  def receive = {

    case GetLoad ⇒ sender ! load

    case CalculateLoad ⇒ scala.concurrent.Future {
      try {
        load = WS.url(router.load).get() map (_.body) map parseIntOption await makeTimeout.seconds(1)
      }
      catch {
        case e: Exception ⇒ {
          // logwarn("[stockfish client calculate load] " + e.getMessage)
          load = none
        }
      }
    }
    case Play(pgn, fen, level) ⇒ WS.url(router.play).withQueryString(
      "pgn" -> pgn,
      "initialFen" -> fen,
      "level" -> level.toString
    ).get() map (_.body) pipeTo sender

    case Analyse(pgn: String, fen: String) ⇒ WS.url(router.analyse).withQueryString(
      "pgn" -> pgn,
      "initialFen" -> fen
    ).get() map (_.body) pipeTo sender
  }

}
