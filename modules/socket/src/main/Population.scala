package lila.socket

import akka.actor._

import actorApi.{ SocketEnter, SocketLeave, PopulationTell, NbMembers }

private[socket] final class Population extends Actor {

  var nb = 0
  val bus = context.system.lilaBus

  override def preStart() {
    bus.subscribe(self, 'socketDoor)
  }

  override def postStop() {
    super.postStop()
    bus.unsubscribe(self)
  }

  def receive = {

    case _: SocketEnter[_] =>
      nb = nb + 1
      lila.mon.socket.open()

    case _: SocketLeave[_] =>
      nb = nb - 1
      lila.mon.socket.close()

    case PopulationTell => bus.publish(NbMembers(nb), 'nbMembers)
  }
}
