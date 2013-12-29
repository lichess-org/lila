package lila

import lila.game.Event
import lila.socket.WithSocket

package object round extends PackageObject with WithPlay with WithSocket {

  private[round] type Events = List[Event]
}

package round {

private[round] class ClientErrorException(e: String) extends Exception(e)

private[round] object ClientErrorException {

  def future(e: String) = fufail(new ClientErrorException(e))
}

}
