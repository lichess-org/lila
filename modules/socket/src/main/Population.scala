package lidraughts.socket

import lidraughts.hub.Trouper
import actorApi.{ SocketEnter, SocketLeave, PopulationTell, NbMembers }

private[socket] final class Population(system: akka.actor.ActorSystem) extends Trouper {

  private var nb = 0

  system.lidraughtsBus.subscribe(this, 'socketEnter, 'socketLeave)

  val process: Trouper.Receive = {

    case _: SocketEnter[_] =>
      nb = nb + 1
      lidraughts.mon.socket.open()

    case _: SocketLeave[_] =>
      nb = nb - 1
      lidraughts.mon.socket.close()

    case PopulationTell => system.lidraughtsBus.publish(NbMembers(nb), 'nbMembers)
  }
}
