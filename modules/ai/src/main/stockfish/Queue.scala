package lila.ai
package stockfish

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._
import lila.analyse.{ AnalysisMaker, Info }

private[ai] final class Queue(config: Config) extends Actor {

  private val process = Process(config.execPath, "stockfish") _
  private val actor = context.actorOf(Props(new ActorFSM(process, config)))
  private var actorReady = false

  def receive = {

    case req: PlayReq ⇒ {
      implicit def timeout = makeTimeout((config moveTime req.level).millis + 100.millis)
      actor ? req mapTo manifest[Valid[String]] map {
        case scalaz.Success(move) ⇒ sender ! move
        case failure              ⇒ logwarn("[ai] stockfish play " + failure.toString)
      } await timeout
    }

    case req: AnalReq ⇒ {
      implicit def timeout = makeTimeout(config.analyseMoveTime + 100.millis)
      (actor ? req) mapTo manifest[Valid[Int ⇒ Info]] map {
        case scalaz.Success(info) ⇒ sender ! info
        case failure              ⇒ logwarn("[ai] stockfish analyse " + failure.toString)
      } await timeout
    }

    case FullAnalReq(moveString, fen) ⇒ {
      implicit def timeout = makeTimeout(config.analyseTimeout)
      val moves = moveString.split(' ').toList
      ((1 to moves.size - 1).toList map moves.take map { serie ⇒
        self ? AnalReq(serie mkString " ", fen)
      }).sequence mapTo manifest[List[Int ⇒ Info]] map { infos ⇒
        AnalysisMaker(infos.zipWithIndex map (x ⇒ x._1 -> (x._2 + 1)) map {
          case (info, turn) ⇒ (turn % 2 == 0).fold(
            info(turn),
            info(turn) |> { i ⇒ i.copy(score = i.score map (_.negate)) }
          ).pp
        }, true, none)
      } pipeTo sender
    }
  }
}
