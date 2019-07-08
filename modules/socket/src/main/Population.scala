package lila.socket

import lila.hub.Trouper
import actorApi.{ SocketEnter, SocketLeave, PopulationTell, NbMembers, RemoteNbMembers }

private[socket] final class Population(system: akka.actor.ActorSystem) extends Trouper {

  private var nb = 0
  private var remoteNb = 0

  system.lilaBus.subscribe(this, 'socketEnter, 'socketLeave)

  val process: Trouper.Receive = {

    case _: SocketEnter =>
      nb = nb + 1
      lila.mon.socket.open()

    case _: SocketLeave =>
      nb = nb - 1
      lila.mon.socket.close()

    case RemoteNbMembers(r) => remoteNb = r

    case PopulationTell => system.lilaBus.publish(NbMembers(nb + remoteNb), 'nbMembers)
  }
}
