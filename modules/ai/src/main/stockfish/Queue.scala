package lila.ai
package stockfish

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout

import actorApi._
import lila.analyse.{ Evaluation, Info }
import lila.hub.actorApi.ai.GetLoad

private[ai] final class Queue(config: Config) extends Actor {

  private val process = Process(config.execPath, "stockfish") _
  private val actor = context.actorOf(Props(new ActorFSM(process, config)))
  private val monitor = context.actorOf(Props(new Monitor(self)))
  private var actorReady = false
  private val extraStockfishTime = 2 seconds

  private def blockAndMeasure[A](fa: Fu[A])(implicit timeout: Timeout): A = {
    val start = nowMillis
    val result = fa await timeout
    monitor ! AddTime((nowMillis - start).toInt)
    result
  }

  def receive = {

    case GetLoad => {
      import makeTimeout.short
      monitor ? GetLoad pipeTo sender
    }

    case req: PlayReq => {
      implicit val timeout = makeTimeout((config moveTime req.level).millis + extraStockfishTime)
      blockAndMeasure {
        actor ? req mapTo manifest[Option[String]] map sender.!
      }
    }

    case req: AnalReq =>
      if (req.isStart) sender ! Evaluation.start.some
      else {
        implicit val timeout = makeTimeout(config.analyseMoveTime + extraStockfishTime)
        (actor ? req) map sender.! await timeout
      }

    case FullAnalReq(moves, fen) => {
      val mrSender = sender
      implicit val timeout = makeTimeout(config.analyseTimeout)
      val futures = (0 to moves.size).toStream map moves.take map { serie =>
        self ? AnalReq(serie, fen) mapTo manifest[Option[Evaluation]]
      }
      lila.common.Future.lazyFold(futures)(Vector[Option[Evaluation]]())(_ :+ _) addFailureEffect {
        case e => mrSender ! Status.Failure(e)
      } foreach { results =>
        mrSender ! Evaluation.toInfos(results.toList.map(_ | Evaluation.empty), moves)
      }
    }
  }
}
