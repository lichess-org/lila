package lila.ai

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.routing._
import akka.util.Timeout

import actorApi._
import lila.analyse.{ Evaluation, Info }

private[ai] final class Queue(config: Config) extends Actor {

  private val waiters =
    context.actorOf(Props(classOf[Waiter], config)
      .withRouter(SmallestMailboxRouter(
        config.nbInstances,
        supervisorStrategy = OneForOneStrategy() {
          case e: java.util.concurrent.TimeoutException => SupervisorStrategy.Restart
          case _                                        => SupervisorStrategy.Escalate
        })))

  private val extraStockfishTime = 2 seconds

  def receive = {

    case req: PlayReq =>
      waiters ! Waiter.Task(req, sender, makeTimeout((config moveTime req.level).millis + extraStockfishTime))

    case req: AnalReq =>
      if (req.isStart) sender ! Evaluation.start.some
      else waiters ! Waiter.Task(req, sender, makeTimeout(config.analyseMoveTime + extraStockfishTime))

    case FullAnalReq(moves, fen) => {
      val mrSender = sender
      implicit val timeout = makeTimeout(config.analyseTimeout)
      val futures = (0 to moves.size) map moves.take map { serie =>
        self ? AnalReq(serie, fen) mapTo manifest[Option[Evaluation]]
      }
      Future.fold(futures)(Vector[Option[Evaluation]]())(_ :+ _) addFailureEffect {
        case e => mrSender ! Status.Failure(e)
      } foreach { results =>
        mrSender ! Evaluation.toInfos(results.toList.map(_ | Evaluation.empty), moves)
      }
    }
  }
}

import akka.dispatch.PriorityGenerator
import akka.dispatch.UnboundedPriorityMailbox
import com.typesafe.config.{ Config => TypesafeConfig }

// We inherit, in this case, from UnboundedPriorityMailbox
// and seed it with the priority generator
final class QueueMailBox(settings: ActorSystem.Settings, config: TypesafeConfig)
  extends UnboundedPriorityMailbox(PriorityGenerator {
    case PlayReq(_, _, level) => level
    case _: AnalReq           => 10
    case _                    => 20
  })
