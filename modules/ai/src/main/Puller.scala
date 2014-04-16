package lila.ai

import actorApi._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }

private[ai] final class Puller(
  config: Config,
  id: Int,
  logger: String => Unit) extends Actor {

  import Puller._

  private val name: String = s"fsm-$id"
  private val master = context.parent
  private val worker: ActorRef = context.actorOf(
    Props(classOf[ActorFSM], name, Process(config.execPath, s"proc-$id") _, config, logger),
    name = name)

  private def pull {
    context.system.scheduler.scheduleOnce(200 milliseconds, master, GimmeWork)
  }

  def idle: Receive = {

    case NoWork => pull

    case task: Task =>
      context become busy
      implicit val tm = task.timeout
      worker ? task.req onComplete { res =>
        self ! Complete(task, res)
      }

    case c: Complete => play.api.Logger(name).warn("Received complete while idle!")
  }

  def busy: Receive = {

    case Complete(task, Success(res)) =>
      context become idle
      task.replyTo ! res
      master ! GimmeWork

    case Complete(task, Failure(err)) =>
      task.retry match {
        case Some(retry) => master ! Enqueue(retry)
        case _           => task.replyTo ! Status.Failure(err)
      }
      throw err

    case t: Task =>
      play.api.Logger(name).warn("Received task while busy! Sending back to master...")
      master ! Enqueue(t)

    case NoWork => play.api.Logger(name).warn("Received nowork while busy!")
  }

  def receive = idle

  override def preStart() {
    master ! GimmeWork
  }
}

private[ai] object Puller {

  case object GimmeWork
  case object NoWork
  case class Complete(task: Task, res: Try[Any])
  case class Enqueue(task: Task)

  case class Task(
      req: Req,
      replyTo: ActorRef,
      timeout: Timeout,
      isRetry: Boolean = false) extends Ordered[Task] {

    def retry = !isRetry option copy(isRetry = true)

    def compare(other: Task): Int = this.req.priority compare other.req.priority
  }
}
