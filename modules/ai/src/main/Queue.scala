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

  import Puller._

  import akka.actor.SupervisorStrategy._

  override val supervisorStrategy = OneForOneStrategy() {
    case e: Exception => Restart
  }

  private case class Log(msg: String)
  private val logger = (msg: String) => self ! Log(msg)

  (1 to config.nbInstances).toList map { id =>
    context.actorOf(Props(classOf[Puller], config, id, logger), name = s"pull-$id")
  }

  private val extraStockfishTime = 1 second

  private val tasks = new scala.collection.mutable.PriorityQueue[Task]

  private val maxTasks = 500 * config.nbInstances

  def receive = {

    case Log(msg) =>
      val nbByHuman = tasks.count(_.req.requestedByHuman)
      val nbAuto = tasks.size - nbByHuman
      println(s"[$nbByHuman/$nbAuto] $msg")

    case Enqueue(task) => tasks += task

    case GimmeWork =>
      if (tasks.isEmpty) sender ! NoWork
      else sender ! tasks.dequeue

    case req: PlayReq =>
      val timeout = makeTimeout((config moveTime req.level).millis + extraStockfishTime)
      tasks += Task(req, sender, timeout)

    case req: AnalReq =>
      if (req.isStart) sender ! Evaluation.start.some
      else {
        val timeout = makeTimeout(config.analyseMoveTime + extraStockfishTime)
        tasks += Task(req, sender, timeout)
      }

    case FullAnalReq(moves, fen, requestedByHuman) if (requestedByHuman || tasks.size < maxTasks) =>
      val mrSender = sender
      val size = moves.size
      implicit val timeout = makeTimeout {
        if (requestedByHuman) 1.hour else 24.hours
      }
      val futures = (0 to size) map moves.take map { serie =>
        self ? AnalReq(serie, fen, size, requestedByHuman) mapTo manifest[Option[Evaluation]]
      }
      Future.fold(futures)(Vector[Option[Evaluation]]())(_ :+ _) addFailureEffect {
        case e => mrSender ! Status.Failure(e)
      } foreach { results =>
        mrSender ! Evaluation.toInfos(results.toList.map(_ | Evaluation.empty), moves)
      }
  }
}
