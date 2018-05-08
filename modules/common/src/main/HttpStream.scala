package lidraughts.common

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

object HttpStream {

  def onComplete(stream: Option[ActorRef], system: ActorSystem) =
    stream foreach { actor =>
      system.lidraughtsBus.unsubscribe(actor)
      actor ! PoisonPill
    }

  case object SetOnline
}
