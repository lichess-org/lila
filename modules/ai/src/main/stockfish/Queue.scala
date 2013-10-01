package lila.ai
package stockfish

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout

import actorApi._
import lila.analyse.{ AnalysisMaker, Info }
import lila.hub.actorApi.ai.GetLoad

private[ai] final class Queue(config: Config, system: ActorSystem) extends Actor {

  private val process = Process(config.execPath, "stockfish") _
  private val actor = context.actorOf(Props(new ActorFSM(process, config)))
  private val monitor = context.actorOf(Props(new Monitor(self)))
  private var actorReady = false

  private def blockAndMeasure[A](fa: Fu[A])(implicit timeout: Timeout): A = {
    val start = nowMillis
    val result = fa await timeout
    monitor ! AddTime((nowMillis - start).toInt)
    result
  }

  def receive = {

    case GetLoad ⇒ {
      import makeTimeout.short
      monitor ? GetLoad pipeTo sender
    }

    case req: PlayReq ⇒ {
      implicit val timeout = makeTimeout((config moveTime req.level).millis + 2.second)
      blockAndMeasure {
        actor ? req mapTo manifest[Valid[String]] map sender.!
      }
    }

    case req: AnalReq ⇒ {
      implicit val timeout = makeTimeout(config.analyseMoveTime + 2.second)
      (actor ? req) mapTo manifest[Valid[Int ⇒ Info]] map sender.! await timeout
    }

    case FullAnalReq(moveString, fen) ⇒ {
      implicit def timeout = makeTimeout(config.analyseTimeout)
      type Result = Valid[Int ⇒ Info]
      val moves = moveString.split(' ').toList
      val futures = (1 to moves.size - 1).toStream map moves.take map { serie ⇒
        self ? AnalReq(serie.init mkString " ", serie.last, fen) mapTo manifest[Result]
      }
      lila.common.Future.lazyFold(futures)(List[Result]())(_ :+ _) addFailureEffect {
        case e ⇒ sender ! Status.Failure(e)
      } map {
        _.sequence map { infos ⇒
          AnalysisMaker(infos.zipWithIndex map (x ⇒ x._1 -> (x._2 + 1)) map {
            case (info, turn) ⇒ (turn % 2 == 1).fold(
              info(turn),
              info(turn) |> { i ⇒ i.copy(score = i.score map (_.negate)) }
            )
          }, true, none)
        }
      } pipeTo sender
    }
  }

  system.scheduler.schedule(1 second, 1 seconds) {
    import makeTimeout.short
    self ? GetLoad foreach {
      case 0         ⇒
      case load: Int ⇒ println("[stockfish] load = " + load)
    }
  }
}
