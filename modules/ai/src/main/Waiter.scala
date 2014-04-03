package lila.ai

import actorApi._

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

import akka.dispatch.PriorityGenerator
import akka.dispatch.UnboundedPriorityMailbox
import com.typesafe.config.{ Config => TypesafeConfig }

// We inherit, in this case, from UnboundedPriorityMailbox
// and seed it with the priority generator
final class WaiterMailBox(settings: ActorSystem.Settings, config: TypesafeConfig)
  extends UnboundedPriorityMailbox(PriorityGenerator {
    case PlayReq(_, _, level) => level
    case _: AnalReq           => 10
    case _                    => 20
  })
