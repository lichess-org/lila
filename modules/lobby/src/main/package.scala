package lila

import lila.socket.WithSocket

package object lobby extends PackageObject with WithPlay with WithSocket {

  object tube {

    private[lobby] implicit lazy val messageTube = 
      Message.tube inColl Env.current.messageColl
  }
}
