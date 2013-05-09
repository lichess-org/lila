package lila

import lila.socket.WithSocket
import lila.game.Event

package object round extends PackageObject with WithPlay with WithSocket {

  object tube {
    
    implicit lazy val roomTube = Room.tube inColl Env.current.roomColl

    implicit lazy val watcherRoomTube = WatcherRoom.tube inColl Env.current.watcherRoomColl
  }

  private[round] type Events = List[Event]
  private[round] type FuEvents = Fu[Events]
}
