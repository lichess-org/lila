package lila.ai

import actorApi._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }

private[ai] final class Puller(config: Config, id: Int) extends Actor {

  import Puller._

  private val name: String = s"sf-$id"
  private val master = context.parent
  private val worker: ActorRef = context.actorOf(
    Props(new ActorFSM(name, Process(config.execPath, s"SF $id"), config)),
    name = name)

  private def pull {
    context.system.scheduler.scheduleOnce(200 milliseconds, master, GimmeWork)
  }

  def receiveIdle: Receive = {

    case NoWork => pull

    case Task(req, replyTo, timeout, _, _) =>
      context become receiveBusy
      implicit val tm = timeout
      worker ? req onComplete { res =>
        self ! Complete(res, replyTo)
      }

    case c: Complete => play.api.Logger(name).warn("Received complete while idle!")
  }

  def receiveBusy: Receive = {

    case Complete(Success(res), replyTo) =>
      replyTo ! res
      context become receiveIdle
      master ! GimmeWork

    case Complete(Failure(err), replyTo) =>
      replyTo ! Status.Failure(err)
      throw err

    case t: Task =>
      play.api.Logger(name).warn("Received task while busy! Sending back to master...")
      master ! Enqueue(t)
    case NoWork => play.api.Logger(name).warn("Received nowork while busy!")
  }

  def receive = receiveIdle

  override def preStart() {
    master ! GimmeWork
  }
}

private[ai] object Puller {

  case object GimmeWork
  case object NoWork
  case class Complete(res: Try[Any], replyTo: ActorRef)
  case class Enqueue(task: Task)

  case class Task(
      req: Req,
      replyTo: ActorRef,
      timeout: Timeout,
      date: Int = nowSeconds,
      attempts: Int = 0) extends Ordered[Task] {

    def again = copy(attempts = attempts + 1)

    def priority = req match {
      case _: PlayReq => 20
      case _: AnalReq => 10
    }

    def compare(other: Task): Int = priority compare other.priority match {
      case 0 => other.date compare date
      case x => x
    }
  }
}
