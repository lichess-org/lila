package lila.socket

import akka.actor._

import actorApi.{ PopulationInc, PopulationDec, PopulationGet }

private[socket] final class Population extends Actor {

  private var nb = 0

  List(PopulationInc.getClass, PopulationDec.getClass) foreach { klass ⇒
    context.system.eventStream.subscribe(self, klass)
  }

  override def postStop() {
    context.system.eventStream.unsubscribe(self)
  }

  def receive = {

    case PopulationInc ⇒ nb = nb + 1
    case PopulationDec ⇒ nb = nb - 1

    case PopulationGet ⇒ sender ! nb
  }
}
