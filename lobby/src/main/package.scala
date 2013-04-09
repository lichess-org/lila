package lila

import lila.socket.WithSocket

package object lobby extends PackageObject with WithPlay with WithSocket {

  object tube {

    implicit lazy val hookTube = 
      Hook.tube inColl Env.current.hookColl

    private[lobby] implicit lazy val messageTube = 
      Message.tube inColl Env.current.messageColl
  }
}
