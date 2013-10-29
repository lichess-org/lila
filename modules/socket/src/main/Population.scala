package lila.socket

import akka.actor._

import actorApi.{ PopulationInc, PopulationDec, PopulationTell, NbMembers }

private[socket] final class Population extends Actor {

  var nb = 0
  val bus = context.system.lilaBus

  bus.subscribe(self, 'population)

  override def postStop() {
    bus.unsubscribe(self)
  }

  def receive = {

    case PopulationInc ⇒ nb = nb + 1
    case PopulationDec ⇒ nb = nb - 1

    case PopulationTell ⇒ bus.publish(NbMembers(nb), 'nbMembers)
  }
}
