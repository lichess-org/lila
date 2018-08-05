package lila.common

import akka.actor._

object HttpStream {

  def onComplete(stream: Option[ActorRef], system: ActorSystem) =
    stream foreach { actor =>
      system.lilaBus.unsubscribe(actor)
      actor ! PoisonPill
    }

  case object SetOnline
}
