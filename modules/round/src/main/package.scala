package lila

import lila.game.Event
import lila.socket.WithSocket

package object round extends PackageObject with WithPlay with WithSocket {

  private[round] object tube {

    implicit lazy val roomTube = Room.tube inColl Env.current.roomColl

    implicit lazy val watcherRoomTube = WatcherRoom.tube inColl Env.current.watcherRoomColl
  }

  private[round] type Events = List[Event]
}

package round {

private[round] class ClientErrorException(e: String) extends Exception(e)

private[round] object ClientErrorException {

  def future(e: String) = fufail(new ClientErrorException(e))
}

}
