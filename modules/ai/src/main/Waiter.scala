package lila.ai

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

private[ai] final class Waiter(config: Config) extends Actor {

  import Waiter.Task

  private val name: String = s"SF ${ornicar.scalalib.Random nextString 3}"
  private val worker: ActorRef = context.actorOf(Props(new ActorFSM(name, Process(config.execPath, name), config)))

  def receive = {

    case Task(msg, replyTo, timeout, _) =>
      implicit val tm = timeout
      (worker ? msg map replyTo.!) await tm
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    message match {
      case Some(Task(msg, _, _, a)) if a > 1 => play.api.Logger("[ai] give up " + msg)
      case Some(t: Task)                     => self forward t.again
      case _                                 =>
    }
    super.preRestart(reason, message)
  }
}

private[ai] object Waiter {

  case class Task(msg: Any, replyTo: ActorRef, timeout: Timeout, attempts: Int = 0) {

    def again = copy(attempts = attempts + 1)
  }
}
