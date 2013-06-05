package lila.ai
package stockfish

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._

private[stockfish] final class Queue(config: Config) extends Actor {

  private val playTimeout = makeTimeout(config.playMaxMoveTime + 100.millis)
  private val process = Process(config.execPath, "StockFish") _
  private val actor = context.actorOf(Props(new ActorFSM(process, config)))
  private var actorReady = false

  def receive = {

    case req: Req ⇒ {
      implicit def timeout = playTimeout
      actor ? req map {
        case bestMove: BestMove ⇒ sender ! ~bestMove.move
      }
    } await playTimeout
  }
}
