package lila

import lila.socket.WithSocket

package object round extends PackageObject with WithPlay with WithSocket {

  object tube {
    
    implicit lazy val roomTube = Room.tube inColl Env.current.roomColl

    implicit lazy val watcherRoomTube = WatcherRoom.tube inColl Env.current.watcherRoomColl
  }
}
